package bio.overture.ego.utils;

import bio.overture.ego.model.dto.PermissionRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.val;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.utils.Joiners.COMMA;
import static bio.overture.ego.utils.PermissionRequestAnalyzer.REQUEST_TYPE.DUPLICATE;
import static bio.overture.ego.utils.PermissionRequestAnalyzer.REQUEST_TYPE.NEW;
import static bio.overture.ego.utils.PermissionRequestAnalyzer.REQUEST_TYPE.UPDATE;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Maps.uniqueIndex;
import static java.lang.String.format;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class PermissionRequestAnalyzer {

  /**
   * Constants
   */
  private static final List<PermissionRequest> EMPTY_PERMISSION_REQUEST_LIST = ImmutableList.of();

  public enum REQUEST_TYPE {
    DUPLICATE,
    NEW,
    UPDATE;
  }

  /**
   * Analyzes permission requests by comparing the {@param rawPermissionRequests} to {@param existingPermissionRequests}
   * and categorizes them based on their { @code REQUEST_TYPE } and packs it all into a { @code PermissionAnalysis }.
   * @param existingPermissionRequests collection of PermissionRequests that already exist
   * @param rawPermissionRequests collection of PermissionRequests to analyze against the existing ones
   * @return PermissionAnalysis
   */
  public static PermissionAnalysis analyze(
      @NonNull Collection<PermissionRequest> existingPermissionRequests,
      @NonNull Collection<PermissionRequest> rawPermissionRequests ){
    val existingPermissionRequestIndex = uniqueIndex(existingPermissionRequests, PermissionRequest::getPolicyId);

    val unresolvableRequestMap = filterUnresolvableRequests(rawPermissionRequests);
    val typeMap = rawPermissionRequests.stream()
        .filter(x -> !unresolvableRequestMap.containsKey(x.getPolicyId()))
        .collect(groupingBy(x -> resolvePermType(existingPermissionRequestIndex, x)));

    return PermissionAnalysis.builder()
        .unresolvableMap(unresolvableRequestMap)
        .duplicates(extractPermissionRequests(typeMap, DUPLICATE))
        .createables(extractPermissionRequests(typeMap, NEW))
        .updateables(extractPermissionRequests(typeMap, UPDATE))
        .build();
  }

  private static Set<PermissionRequest> extractPermissionRequests(
      Map<REQUEST_TYPE, List<PermissionRequest>> typeMap,
      REQUEST_TYPE permType){
    return ImmutableSet.copyOf(typeMap.getOrDefault(permType, EMPTY_PERMISSION_REQUEST_LIST));
  }

  private static REQUEST_TYPE resolvePermType(Map<UUID, PermissionRequest> existingPermissionRequestIndex, PermissionRequest r){
    if (existingPermissionRequestIndex.containsValue(r)){
      return DUPLICATE;
    } else if (existingPermissionRequestIndex.containsKey(r.getPolicyId())){
      return UPDATE;
    } else {
      return NEW;
    }
  }

  private static Map<UUID, List<PermissionRequest>> filterUnresolvableRequests(
      @NonNull Collection<PermissionRequest> rawPermissionRequests){
    val grouping = rawPermissionRequests.stream()
        .collect(groupingBy(PermissionRequest::getPolicyId));
    val unresolvableRequestMap = newHashMap(grouping);
    grouping.values()
        .stream()
        // filter aggregates that have multiple permissions for the same policyID
        .filter(x -> x.size()==1)
        .map(x -> x.get(0))
        .map(PermissionRequest::getPolicyId)
        .forEach(unresolvableRequestMap::remove);
    return ImmutableMap.copyOf(unresolvableRequestMap);
  }

  @Value
  @Builder
  public static class PermissionAnalysis {

    private static final String SEP = " , ";

    @NonNull private final Map<UUID, List<PermissionRequest>> unresolvableMap;
    @NonNull private final Set<PermissionRequest> duplicates;
    @NonNull private final Set<PermissionRequest> createables;
    @NonNull private final Set<PermissionRequest> updateables;

    public Optional<String> summarizeUnresolvables(){
      if (unresolvableMap.isEmpty()){
        return Optional.empty();
      }
      return Optional.of(
          unresolvableMap.entrySet()
              .stream()
              .map(this::createDescription)
              .collect(joining(SEP)));
    }

    private String createDescription(Map.Entry<UUID, List<PermissionRequest>> entry){
      val policyId = entry.getKey();
      val unresolvablePermissionRequests = entry.getValue();
      val masks = mapToSet(unresolvablePermissionRequests, PermissionRequest::getMask);
      return format("%s : [%s]", policyId, COMMA.join(masks));
    }
  }

}

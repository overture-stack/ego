package bio.overture.ego.service;

import bio.overture.ego.model.dto.PermissionRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static bio.overture.ego.service.PermissionRequestAnalyzer.REQUEST_TYPE.DUPLICATE;
import static bio.overture.ego.service.PermissionRequestAnalyzer.REQUEST_TYPE.NEW;
import static bio.overture.ego.service.PermissionRequestAnalyzer.REQUEST_TYPE.UPDATE;
import static bio.overture.ego.utils.CollectionUtils.mapToSet;
import static bio.overture.ego.utils.Joiners.COMMA;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static lombok.AccessLevel.PRIVATE;

/**
 * Analyzes permission requests by comparing them to existing permission requests,
 * and categorizes them based on their { @code REQUEST_TYPE } and packs it all into a { @code PermissionAnalysis }
 */
@RequiredArgsConstructor(access = PRIVATE)
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
   * Dependencies
   */
  private final Map<UUID, PermissionRequest> existingPermissionRequests;

  public PermissionAnalysis analyze(
      @NonNull Collection<PermissionRequest> rawPermissionRequests ){
    val unresolvableRequestMap = filterUnresolvableRequests(rawPermissionRequests);
    val typeMap = rawPermissionRequests.stream()
        .filter(x -> !unresolvableRequestMap.containsKey(x.getPolicyId()))
        .collect(groupingBy(this::resolvePermType));

    return PermissionAnalysis.builder()
        .unresolvableMap(unresolvableRequestMap)
        .duplicates(extractPermissionRequests(typeMap, DUPLICATE))
        .createables(extractPermissionRequests(typeMap, NEW))
        .updateables(extractPermissionRequests(typeMap, UPDATE))
        .build();
  }

  private Set<PermissionRequest> extractPermissionRequests(Map<REQUEST_TYPE, List<PermissionRequest>> typeMap, REQUEST_TYPE permType){
    return ImmutableSet.copyOf(typeMap.getOrDefault(permType, EMPTY_PERMISSION_REQUEST_LIST));
  }

  private REQUEST_TYPE resolvePermType(PermissionRequest r){
    if (existingPermissionRequests.containsValue(r)){
      return DUPLICATE;
    } else if (existingPermissionRequests.containsKey(r.getPolicyId())){
      return UPDATE;
    } else {
      return NEW;
    }
  }

  public static PermissionRequestAnalyzer createFromExistingPermissionRequests(Collection<PermissionRequest> existingPermissionRequests){
    val existing = existingPermissionRequests.stream()
        .collect(toMap(PermissionRequest::getPolicyId, identity()));
    return new PermissionRequestAnalyzer(existing);
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
    @NonNull private final Map<UUID, List<PermissionRequest>> unresolvableMap;
    @NonNull private final Set<PermissionRequest> duplicates;
    @NonNull private final Set<PermissionRequest> createables;
    @NonNull private final Set<PermissionRequest> updateables;

    public Optional<String> summarizeUnresolvables(){
      if (unresolvableMap.isEmpty()){
        return Optional.empty();
      }

      val statements = unresolvableMap.entrySet()
          .stream()
          .map(this::convert)
          .collect(toSet());
      return Optional.of(COMMA.join(statements));
    }

    private String convert(Map.Entry<UUID, List<PermissionRequest>> entry){
      val unresolvablePermissionRequests = entry.getValue();
      val policyId = entry.getKey();
      val masks = mapToSet(entry.getValue(), PermissionRequest::getMask);
      return format("%s : [%s]", policyId, COMMA.join(masks));
    }
  }

}

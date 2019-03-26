package bio.overture.ego.utils;

import static bio.overture.ego.model.enums.LanguageType.ENGLISH;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.utils.CollectionUtils.listOf;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.Splitters.COMMA_SPLITTER;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.abs;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import bio.overture.ego.model.dto.CreateApplicationRequest;
import bio.overture.ego.model.dto.CreateUserRequest;
import bio.overture.ego.model.dto.GroupRequest;
import bio.overture.ego.model.dto.PolicyRequest;
import bio.overture.ego.model.dto.Scope;
import bio.overture.ego.model.entity.Application;
import bio.overture.ego.model.entity.Group;
import bio.overture.ego.model.entity.Policy;
import bio.overture.ego.model.entity.Token;
import bio.overture.ego.model.entity.User;
import bio.overture.ego.model.entity.UserPermission;
import bio.overture.ego.model.enums.AccessLevel;
import bio.overture.ego.model.enums.ApplicationType;
import bio.overture.ego.model.enums.LanguageType;
import bio.overture.ego.model.enums.StatusType;
import bio.overture.ego.model.enums.UserType;
import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.service.ApplicationService;
import bio.overture.ego.service.BaseService;
import bio.overture.ego.service.GroupService;
import bio.overture.ego.service.NamedService;
import bio.overture.ego.service.PolicyService;
import bio.overture.ego.service.TokenService;
import bio.overture.ego.service.TokenStoreService;
import bio.overture.ego.service.UserPermissionService;
import bio.overture.ego.service.UserService;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
/**
 * * For this class, we follow the following naming conventions: createEntity: returns a new object
 * of applicationType Entity. setupEntity: Create an policy, saves it using Hibernate, & returns it.
 * setupEntities: Sets up multiple entities at once setupTestEntities: Sets up specific entities
 * used in our unit tests
 */
public class EntityGenerator {

  private static final String DICTIONARY =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890_-abcdefghijklmnopqrstuvwxyz";

  @Autowired private TokenService tokenService;

  @Autowired private ApplicationService applicationService;

  @Autowired private UserService userService;

  @Autowired private GroupService groupService;

  @Autowired private PolicyService policyService;

  @Autowired private TokenStoreService tokenStoreService;

  @Autowired private UserPermissionService userPermissionService;

  public Application setupApplication(String clientId) {
    return applicationService
        .findByClientId(clientId)
        .orElseGet(
            () -> {
              val request = createApplicationCreateRequest(clientId);
              return applicationService.create(request);
            });
  }

  public List<Application> setupApplications(String... clientIds) {
    return mapToList(listOf(clientIds), this::setupApplication);
  }

  public void setupTestApplications(String postfix) {
    setupApplications(
        String.format("111111_%s", postfix),
        String.format("222222_%s", postfix),
        String.format("333333_%s", postfix),
        String.format("444444_%s", postfix),
        String.format("555555_%s", postfix));
  }

  public void setupTestApplications() {
    setupApplications("111111", "222222", "333333", "444444", "555555");
  }

  public Application setupApplication(
      String clientId, String clientSecret, ApplicationType applicationType) {
    return applicationService
        .findByClientId(clientId)
        .orElseGet(
            () -> {
              val request =
                  CreateApplicationRequest.builder()
                      .name(clientId)
                      .type(applicationType)
                      .clientSecret(clientSecret)
                      .clientId(clientId)
                      .status(APPROVED)
                      .build();
              return applicationService.create(request);
            });
  }

  public User setupUser(String name) {
    val names = name.split(" ", 2);
    val userName = String.format("%s%s@domain.com", names[0], names[1]);
    return userService
        .findByName(userName)
        .orElseGet(
            () -> {
              val createUserRequest = createUser(name);
              return userService.create(createUserRequest);
            });
  }

  public List<User> setupUsers(String... users) {
    return mapToList(listOf(users), this::setupUser);
  }

  public void setupTestUsers() {
    setupUsers("First User", "Second User", "Third User");
  }

  public Group setupGroup(String name) {
    return groupService
        .findByName(name)
        .orElseGet(
            () -> {
              val group = createGroupRequest(name);
              return groupService.create(group);
            });
  }

  private CreateUserRequest createUser(String firstName, String lastName) {
    return CreateUserRequest.builder()
        .email(String.format("%s%s@domain.com", firstName, lastName))
        .firstName(firstName)
        .lastName(lastName)
        .status(APPROVED)
        .preferredLanguage(ENGLISH)
        .type(ADMIN)
        .build();
  }

  private CreateUserRequest createUser(String name) {
    val names = name.split(" ", 2);
    return createUser(names[0], names[1]);
  }

  private GroupRequest createGroupRequest(String name) {
    return GroupRequest.builder().name(name).status(PENDING).description("").build();
  }

  public static <E extends Enum<E>> E randomEnum(Class<E> e) {
    val enums = e.getEnumConstants();
    val r = new Random();
    val randomPos = abs(r.nextInt()) % enums.length;
    return enums[randomPos];
  }

  public static StatusType randomStatusType() {
    return randomEnum(StatusType.class);
  }

  private static String internalRandomString(String dictionary, int length) {
    val r = new Random();
    val sb = new StringBuilder();
    r.ints(length, 0, dictionary.length()).map(dictionary::charAt).forEach(sb::append);
    return sb.toString();
  }

  public static String randomStringWithSpaces(int length) {
    val newDictionary = DICTIONARY + " ";
    return internalRandomString(newDictionary, length);
  }

  public static String randomStringNoSpaces(int length) {
    return internalRandomString(DICTIONARY, length);
  }

  public Group generateRandomGroup() {
    val request =
        GroupRequest.builder()
            .name(generateNonExistentName(groupService))
            .status(randomStatusType())
            .description(randomStringWithSpaces(15))
            .build();
    return groupService.create(request);
  }

  public static ApplicationType randomApplicationType() {
    return randomEnum(ApplicationType.class);
  }

  public static UserType randomUserType() {
    return randomEnum(UserType.class);
  }

  public static LanguageType randomLanguageType() {
    return randomEnum(LanguageType.class);
  }

  public static AccessLevel randomAccessLevel() {
    return randomEnum(AccessLevel.class);
  }

  public Application generateRandomApplication() {
    val request =
        CreateApplicationRequest.builder()
            .clientId(randomStringNoSpaces(10))
            .clientSecret(randomStringNoSpaces(10))
            .name(generateNonExistentName(applicationService))
            .type(randomApplicationType())
            .status(randomStatusType())
            .redirectUri("https://ego.com/" + randomStringNoSpaces(7))
            .description(randomStringWithSpaces(15))
            .build();
    return applicationService.create(request);
  }

  private String randomUserEmail() {
    String email;
    Optional<User> result;

    do {
      email = randomStringNoSpaces(5) + "@xyz.com";
      result = userService.findByName(email);
    } while (result.isPresent());

    return email;
  }

  public User generateRandomUser() {
    val request =
        CreateUserRequest.builder()
            .email(randomUserEmail())
            .status(randomStatusType())
            .type(randomUserType())
            .preferredLanguage(randomLanguageType())
            .firstName(randomStringNoSpaces(5))
            .lastName(randomStringNoSpaces(6))
            .build();
    return userService.create(request);
  }

  public Policy generateRandomPolicy() {
    val request = PolicyRequest.builder().name(generateNonExistentName(policyService)).build();
    return policyService.create(request);
  }

  public List<Group> setupGroups(String... groupNames) {
    return mapToList(listOf(groupNames), this::setupGroup);
  }

  public void setupTestGroups(String postfix) {
    setupGroups(
        String.format("Group One_%s", postfix),
        String.format("Group Two_%s", postfix),
        String.format("Group Three_%s", postfix));
  }

  public void setupTestGroups() {
    setupGroups("Group One", "Group Two", "Group Three");
  }

  public Policy setupSinglePolicy(String name) {
    return policyService
        .findByName(name)
        .orElseGet(
            () -> {
              val createRequest = createPolicyRequest(name);
              return policyService.create(createRequest);
            });
  }

  public Policy setupPolicy(String name, String groupName) {
    return policyService
        .findByName(name)
        .orElseGet(
            () -> {
              val createRequest = createPolicyRequest(name);
              return policyService.create(createRequest);
            });
  }

  public Policy setupPolicy(@NonNull String csv) {
    val args = newArrayList(COMMA_SPLITTER.split(csv));
    assertThat(args).hasSize(2);
    val name = args.get(0);
    val groupName = args.get(1);
    return setupPolicy(name, groupName);
  }

  public List<Policy> setupPolicies(String... names) {
    return mapToList(listOf(names), this::setupPolicy);
  }

  public void setupTestPolicies() {
    setupPolicies("Study001,Group One", "Study002,Group Two", "Study003,Group Three");
  }

  public Token setupToken(
      User user, String token, long duration, Set<Scope> scopes, Set<Application> applications) {
    val tokenObject =
        Token.builder()
            .name(token)
            .owner(user)
            .applications(applications == null ? new HashSet<>() : applications)
            .issueDate(Date.from(Instant.now().plusSeconds(duration)))
            .build();

    tokenObject.setScopes(scopes);

    return tokenStoreService.create(tokenObject);
  }

  public void addPermissions(User user, Set<Scope> scopes) {
    val userPermissions =
        scopes
            .stream()
            .map(
                s -> {
                  UserPermission up = new UserPermission();
                  up.setPolicy(s.getPolicy());
                  up.setAccessLevel(s.getAccessLevel());
                  up.setOwner(user);
                  return up;
                })
            .collect(toList());
    userPermissions.forEach(p -> userPermissionService.associatePermission(user, p));
    userService.getRepository().save(user);
  }

  public String generateNonExistentUserName() {
    val r = new Random();
    String name;
    Optional<User> result;

    do {
      name = generateRandomUserName(r, 5);
      result = userService.findByName(name);
    } while (result.isPresent());

    return name;
  }

  public Set<Scope> getScopes(String... scope) {
    return tokenService.getScopes(ImmutableSet.copyOf(scopeNames(scope)));
  }

  private CreateApplicationRequest createApplicationCreateRequest(String clientId) {
    return CreateApplicationRequest.builder()
        .name(createApplicationName(clientId))
        .type(ApplicationType.CLIENT)
        .clientId(clientId)
        .clientSecret(reverse(clientId))
        .status(PENDING)
        .build();
  }

  private String createApplicationName(String clientId) {
    return String.format("Application %s", clientId);
  }

  private String reverse(String value) {
    return new StringBuilder(value).reverse().toString();
  }

  private PolicyRequest createPolicyRequest(String name) {
    return PolicyRequest.builder().name(name).build();
  }

  public static List<ScopeName> scopeNames(String... strings) {
    return mapToList(listOf(strings), ScopeName::new);
  }

  public static <E extends Enum<E>> E randomEnumExcluding(Class<E> enumClass, E enumToExclude) {
    val list =
        stream(enumClass.getEnumConstants()).filter(x -> x != enumToExclude).collect(toList());
    return randomElementOf(list);
  }

  public static <T> T randomNull(Supplier<T> callback) {
    return randomBoundedInt(2) == 0 ? null : callback.get();
  }

  public static int randomBoundedInt(int maxExclusive) {
    return abs(new Random().nextInt()) % maxExclusive;
  }

  public static int randomBoundedInt(int minInclusive, int maxExclusive) {
    assertThat(MAX_VALUE - maxExclusive).isGreaterThan(minInclusive);
    return minInclusive + randomBoundedInt(maxExclusive);
  }

  public static <T> T randomElementOf(List<T> list) {
    return list.get(randomBoundedInt(list.size()));
  }

  public static <T> T randomElementOf(T... objects) {
    return objects[randomBoundedInt(objects.length)];
  }

  public static <T> String generateNonExistentName(NamedService<T, UUID> namedService) {
    val r = new Random();
    String name = generateRandomName(r, 15);
    Optional<T> result = namedService.findByName(name);

    while (result.isPresent()) {
      name = generateRandomName(r, 15);
      result = namedService.findByName(name);
    }
    return name;
  }

  public static <T> UUID generateNonExistentId(BaseService<T, UUID> baseService) {
    UUID id = UUID.randomUUID();
    while (baseService.isExist(id)) {
      id = UUID.randomUUID();
    }
    return id;
  }

  private static String generateRandomName(Random r, int length) {
    val sb = new StringBuilder();
    r.ints(length, 65, 90).forEach(sb::append);
    return sb.toString();
  }

  private static String generateRandomUserName(Random r, int length) {
    val fn = generateRandomName(r, length);
    val ln = generateRandomName(r, length);
    return fn + " " + ln;
  }
}

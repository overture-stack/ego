package bio.overture.ego.utils;

import static bio.overture.ego.model.enums.IdProviderType.GOOGLE;
import static bio.overture.ego.model.enums.LanguageType.ENGLISH;
import static bio.overture.ego.model.enums.StatusType.APPROVED;
import static bio.overture.ego.model.enums.StatusType.PENDING;
import static bio.overture.ego.model.enums.UserType.ADMIN;
import static bio.overture.ego.model.enums.UserType.USER;
import static bio.overture.ego.utils.CollectionUtils.listOf;
import static bio.overture.ego.utils.CollectionUtils.mapToList;
import static bio.overture.ego.utils.Splitters.COMMA_SPLITTER;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.abs;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import bio.overture.ego.model.dto.*;
import bio.overture.ego.model.entity.*;
import bio.overture.ego.model.enums.*;
import bio.overture.ego.model.params.ScopeName;
import bio.overture.ego.service.*;
import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
  @Autowired private GroupPermissionService groupPermissionService;

  @Autowired private PolicyService policyService;

  @Autowired private ApiKeyStoreService apiKeyStoreService;

  @Autowired private UserPermissionService userPermissionService;

  @Autowired private RefreshContextService refreshContextService;

  private int duration;

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

  public Application addUsersToApplication(Collection<User> users, Application app) {
    val appIdList = Arrays.asList(app.getId());

    users.stream()
        .forEach(user -> userService.associateApplicationsWithUser(user.getId(), appIdList));

    return applicationService.getById(app.getId());
  }

  public User setupUser(String name) {
    return setupUser(name, ADMIN, UUID.randomUUID().toString(), GOOGLE);
  }

  public User setupUser(String name, UserType type) {
    return setupUser(name, type, UUID.randomUUID().toString(), GOOGLE);
  }

  public User setupUser(String name, UserType type, String providerId, IdProviderType provider) {
    val names = name.split(" ", 2);
    val userName = String.format("%s%s@domain.com", names[0], names[1]);
    return userService
        .findByName(userName)
        .orElseGet(
            () -> {
              val createUserRequest = createUser(name, type, provider, providerId);
              return userService.create(createUserRequest);
            });
  }

  public List<User> setupUsers(String... users) {
    return mapToList(
        listOf(users), user -> setupUser(user, ADMIN, UUID.randomUUID().toString(), GOOGLE));
  }

  public List<User> setupPublicUsers(String... users) {
    return mapToList(
        listOf(users), user -> setupUser(user, USER, UUID.randomUUID().toString(), GOOGLE));
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

  public Group addUsersToGroup(Collection<User> users, Group group) {
    val userIds = users.stream().map(user -> user.getId()).collect(toList());
    return groupService.associateUsersWithGroup(group.getId(), userIds);
  }

  private CreateUserRequest createUser(
      String firstName,
      String lastName,
      UserType type,
      IdProviderType provider,
      String providerId) {
    return CreateUserRequest.builder()
        .email(String.format("%s%s@domain.com", firstName, lastName))
        .firstName(firstName)
        .lastName(lastName)
        .status(APPROVED)
        .preferredLanguage(ENGLISH)
        .type(type)
        .identityProvider(provider)
        .providerId(providerId)
        .build();
  }

  private CreateUserRequest createUser(
      String name, UserType type, IdProviderType provider, String providerId) {
    val names = name.split(" ", 2);
    return createUser(names[0], names[1], type, provider, providerId);
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
            .identityProvider(GOOGLE)
            .providerId(UUID.randomUUID().toString())
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

  public Policy setupGroupPermission(Group group, Policy policy, AccessLevel level) {

    val permission = PermissionRequest.builder().mask(level).policyId(policy.getId()).build();

    groupPermissionService.addPermissions(group.getId(), Arrays.asList(permission));

    return policy;
  }

  public Policy setupPolicy(@NonNull String csv) {
    val args = newArrayList(COMMA_SPLITTER.split(csv));
    assertEquals(args.size(), 2);
    val name = args.get(0);

    return setupSinglePolicy(name);
  }

  public List<Policy> setupPolicies(String... names) {
    return mapToList(listOf(names), this::setupSinglePolicy);
  }

  public void setupTestPolicies() {
    setupPolicies("Study001", "Study002", "Study003");
  }

  public ApiKey setupApiKey(
      User user,
      String token,
      boolean isRevoked,
      long duration,
      String description,
      Set<Scope> scopes) {
    val tokenObject =
        ApiKey.builder()
            .name(token)
            .isRevoked(isRevoked)
            .owner(user)
            .description(description)
            .issueDate(Date.from(Instant.now()))
            .expiryDate(Date.from(Instant.now().plus(365, ChronoUnit.DAYS)))
            .build();

    tokenObject.setScopes(scopes);

    return apiKeyStoreService.create(tokenObject);
  }

  public void addPermissions(User user, Set<Scope> scopes) {
    val userPermissions =
        scopes.stream()
            .map(
                s -> {
                  UserPermission up = new UserPermission();
                  up.setPolicy(s.getPolicy());
                  up.setAccessLevel(s.getAccessLevel());
                  up.setOwner(user);
                  userPermissionService.getRepository().save(up);
                  return up;
                })
            .collect(toSet());
    user.getUserPermissions().addAll(userPermissions);
    userService.getRepository().save(user);
  }

  public void addPermissionToUsers(Collection<User> users, Policy policy, AccessLevel level) {
    val permission = PermissionRequest.builder().mask(level).policyId(policy.getId()).build();
    users.stream()
        .forEach(
            user -> userPermissionService.addPermissions(user.getId(), Arrays.asList(permission)));
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

  public static <E extends Enum<E>> E randomEnumExcluding(
      @NonNull Class<E> enumClass, @NonNull E enumToExclude) {
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
    assertTrue((MAX_VALUE - maxExclusive) > minInclusive);
    return minInclusive + randomBoundedInt(maxExclusive);
  }

  public static <T> T randomElementOf(List<T> list) {
    return list.get(randomBoundedInt(list.size()));
  }

  public static <T> T randomElementOf(T... objects) {
    return objects[randomBoundedInt(objects.length)];
  }

  public static String generateNonExistentClientId(
      NamedService<Application, UUID> applicationService) {
    val r = new Random();
    String clientId = generateRandomName(r, 15);
    Optional<Application> result = applicationService.findByName(clientId);

    while (result.isPresent()) {
      clientId = generateRandomName(r, 15);
      result = applicationService.findByName(clientId);
    }
    return clientId;
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

  public static <T> String generateNonExistentProviderId(UserService userService) {
    String providerId = UUID.randomUUID().toString();
    while (userService.existsByProviderId(providerId)) {
      providerId = UUID.randomUUID().toString();
    }
    return providerId;
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

  public RefreshToken generateRandomRefreshToken(
      @Value("${refreshToken.durationMs:43200000}") int duration) {
    this.duration = duration;
    val now = Instant.now();
    val expiry = now.plus(duration, ChronoUnit.MILLIS);

    return RefreshToken.builder()
        .jti(UUID.randomUUID())
        .issueDate(Date.from(now))
        .expiryDate(Date.from(expiry))
        .build();
  }

  public User setupUserWithRefreshToken(String username) {
    val user = this.setupUser(username);
    val userToken = tokenService.generateUserToken(user);
    val refreshToken = refreshContextService.createRefreshToken(userToken);
    return refreshToken.getUser();
  }
}

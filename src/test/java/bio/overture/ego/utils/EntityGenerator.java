package bio.overture.ego.utils;

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
import static java.lang.String.format;
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
import bio.overture.ego.token.IDToken;
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

  @Value("${spring.flyway.placeholders.default-provider:GOOGLE}")
  private ProviderType DEFAULT_PROVIDER_TYPE;

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
        format("111111_%s", postfix),
        format("222222_%s", postfix),
        format("333333_%s", postfix),
        format("444444_%s", postfix),
        format("555555_%s", postfix));
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
    return setupUser(name, ADMIN, UUID.randomUUID().toString(), DEFAULT_PROVIDER_TYPE);
  }

  public User setupUser(String name, UserType type) {
    return setupUser(name, type, UUID.randomUUID().toString(), DEFAULT_PROVIDER_TYPE);
  }

  public User setupUser(
      String name, UserType type, String providerSubjectId, ProviderType providerType) {
    return userService
        .findByProviderTypeAndProviderSubjectId(providerType, providerSubjectId)
        .orElseGet(
            () -> {
              val createUserRequest = createUser(name, type, providerType, providerSubjectId);
              return userService.create(createUserRequest);
            });
  }

  public List<User> setupUsers(String... users) {
    return mapToList(
        listOf(users),
        user -> setupUser(user, ADMIN, UUID.randomUUID().toString(), DEFAULT_PROVIDER_TYPE));
  }

  public List<User> setupPublicUsers(String... users) {
    return mapToList(
        listOf(users),
        user -> setupUser(user, USER, UUID.randomUUID().toString(), DEFAULT_PROVIDER_TYPE));
  }

  public List<User> setupTestUsers() {
    return setupUsers("First User", "Second User", "Third User");
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
      ProviderType providerType,
      String providerSubjectId) {
    return CreateUserRequest.builder()
        .email(format("%s%s@domain.com", firstName, lastName))
        .firstName(firstName)
        .lastName(lastName)
        .status(APPROVED)
        .preferredLanguage(ENGLISH)
        .type(type)
        .providerType(providerType)
        .providerSubjectId(providerSubjectId)
        .build();
  }

  private CreateUserRequest createUser(
      String name, UserType type, ProviderType providerType, String providerSubjectId) {
    val names = name.split(" ", 2);
    return createUser(names[0], names[1], type, providerType, providerSubjectId);
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
    List<User> result;

    do {
      email = randomStringNoSpaces(5) + "@xyz.com";
      result = userService.findByEmail(email);
    } while (result.size() > 0);

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
            .providerType(DEFAULT_PROVIDER_TYPE)
            .providerSubjectId(UUID.randomUUID().toString())
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
        format("Group One_%s", postfix),
        format("Group Two_%s", postfix),
        format("Group Three_%s", postfix));
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
    List<User> result;

    do {
      name = generateRandomUserName(r, 5);
      val names = name.split(" ");
      val email = format("%s%s@xyz.com", names[0], names[1]);
      result = userService.findByEmail(email);
    } while (result.size() > 0);

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
    return format("Application %s", clientId);
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

  public static <T> String generateNonExistentProviderSubjectId(UserService userService) {
    String providerSubjectId = UUID.randomUUID().toString();
    while (userService.existsByProviderSubjectId(providerSubjectId)) {
      providerSubjectId = UUID.randomUUID().toString();
    }
    return providerSubjectId;
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

  public IDToken setupUserIDToken(ProviderType providerType, String providerSubjectId) {
    return setupUserIDToken(
        providerType,
        providerSubjectId,
        generateNonExistentUserName(),
        generateNonExistentUserName());
  }

  public IDToken setupUserIDToken(
      ProviderType providerType, String providerSubjectId, String familyName, String givenName) {
    val idToken = new IDToken();
    idToken.setProviderType(providerType);
    idToken.setProviderSubjectId(providerSubjectId);
    idToken.setEmail(format("%s%s@domain.com", givenName, familyName));
    idToken.setFamilyName(familyName);
    idToken.setGivenName(givenName);
    return idToken;
  }

  public IDToken createNewIdToken() {
    val token = new IDToken();

    val names = generateNonExistentUserName().split(" ");
    val firstName = names[0];
    val lastName = names[1];
    token.setProviderType(DEFAULT_PROVIDER_TYPE);
    token.setProviderSubjectId(generateNonExistentProviderSubjectId(userService));
    token.setEmail(format("%s%s@domain.com", firstName, lastName));
    token.setGivenName(firstName);
    token.setFamilyName(lastName);
    return token;
  }

  public ProviderType createNonDefaultProviderType() {
    return randomEnumExcluding(ProviderType.class, DEFAULT_PROVIDER_TYPE);
  }
}

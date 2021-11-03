// package bio.overture.ego.security;
//
// import java.net.URI;
// import java.net.URISyntaxException;
// import lombok.extern.slf4j.Slf4j;
// import lombok.val;
// import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
//// import
// org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
// import org.springframework.boot.context.properties.NestedConfigurationProperty;
// import org.springframework.web.context.request.RequestContextHolder;
// import org.springframework.web.context.request.ServletRequestAttributes;
// import org.springframework.web.util.UriComponentsBuilder;
//
// @Slf4j
// public class OAuth2ClientResources  {
//
//  @NestedConfigurationProperty
//  private Oauth2CLient client =
//      new AuthorizationCodeResourceDetails() {
//        // Do not send url parameter (including the application id of ego) to authorization server
//        // because some authorization server like google does not support parameter in redirect
// url
//        @Override
//        public String getRedirectUri(AccessTokenRequest request) {
//          try {
//            val uri = new URI(request.getCurrentUri());
//            val attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
//            val session = attr.getRequest().getSession(true);
//
//            val uriComponents = UriComponentsBuilder.fromUri(uri).build();
//            val queryParams = uriComponents.getQueryParams();
//
//            val clientId = queryParams.getFirst("client_id");
//            if (clientId != null && !clientId.isEmpty()) {
//              session.setAttribute("ego_client_id", clientId);
//            }
//
//            val redirectUri = queryParams.getFirst("redirect_uri");
//            if (redirectUri != null && !redirectUri.isEmpty()) {
//              session.setAttribute("ego_redirect_uri", redirectUri);
//            } else {
//              session.setAttribute("ego_redirect_uri", "");
//            }
//
//            if (getPreEstablishedRedirectUri() != null) {
//              return getPreEstablishedRedirectUri();
//            }
//
//            return new URI(
//                    uri.getScheme(), uri.getAuthority(), uri.getPath(), null, uri.getFragment())
//                .toString();
//          } catch (URISyntaxException e) {
//            return request.getCurrentUri();
//          }
//        }
//      };
//
//  @NestedConfigurationProperty
//  private OAuth2ClientProperties.Provider resource = new OAuth2ClientProperties.Provider();
//
//  public AuthorizationCodeResourceDetails getClient() {
//    return client;
//  }
////
//  public OAuth2ClientProperties.Provider getResource() {
//    return resource;
//  }
//
// }
//

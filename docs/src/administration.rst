(1) Admin installs Ego.
(2) Admin inserts a new user with their own Oauth Id into the egousers table, with role ADMIN.
(3) Developer writes an application which requires a given scope.
(4) Admin creates a new policy with the required policy name.
(5) Admin adds application with client id and password, tells them to developer.
(6) Developer configures the application to send a "Basic" application auth token (ie. a Base64 encoded client id and password) in the header in it's REST request to EGO's "check_token" endpoint whenever it needs access to the given scope.
(7) Admin creates users, and optionally assigns them to groups 
(8) Admin then grants access the required scopes to the desired users, either as individuals, or as members of a group. 
(9) Ego User may now sign into Ego, and get an authorization token.
(10) User sends program an ego authorization token; program then does it's call to Ego to get the user's id and allowed scopes. 

(11) Ego checks to make sure that the application is allowed to have the information associated with this token, and that the client id and password is correct.(12) If everything is okay, it sends the application the user's id and the scopes that are authorized by the token.

(13) The program then check that scopes it needs are available, and if so, knows
that it is authorized to handle the request for the user with the given id, and does so.

/* WARNING: Clears all data in the EgoApplication Table

Clears the EgoApplication table and insert 4 sample Applications (3 Approved, 1 Pending)
*/
TRUNCATE public.egoapplication CASCADE;

INSERT INTO EGOAPPLICATION (appName, clientId, clientSecret, redirectUri, description, status) VALUES ('Example Data Portal', 'sample-data-portal', 'sample-data-portal-secret', 'http://google.com', 'Sample application for some data portal', 'Approved');
INSERT INTO EGOAPPLICATION (appName, clientId, clientSecret, redirectUri, description, status) VALUES ('Personal Information Manager', 'personal-info-manager', 'personal-info-manager-secret', 'http://yahoo.com', 'Sample application for some user manager', 'Approved');
INSERT INTO EGOAPPLICATION (appName, clientId, clientSecret, redirectUri, description, status) VALUES ('Daily News Feed', 'daily-news-feed', 'daily-news-feed-secret', 'http://bing.com', 'Sample application for some news feed', 'Approved');
INSERT INTO EGOAPPLICATION (appName, clientId, clientSecret, redirectUri, description, status) VALUES ('User Notification System', 'user-notification-system', 'user-notification-system-secret', 'http://aol.com', 'Sample application for a user notification management system', 'Pending');
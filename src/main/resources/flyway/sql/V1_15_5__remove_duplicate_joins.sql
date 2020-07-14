/*
 * Copyright (c) 2020. The Ontario Institute for Cancer Research. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

/**
  ************************************
  * USER GROUP
  ************************************
 */
CREATE TEMPORARY TABLE user_group_dup AS
SELECT
    user_group_counts.user_id,
    user_group_counts.group_id
FROM
    (SELECT user_id,
            group_id,
            count((user_id, group_id)) as count
     FROM usergroup
     GROUP BY user_id, group_id
     ORDER BY count((user_id, group_id)) DESC
    ) user_group_counts
WHERE user_group_counts.count > 1;

DELETE FROM usergroup
    USING user_group_dup
WHERE usergroup.user_id=user_group_dup.user_id AND usergroup.group_id=user_group_dup.group_id;

INSERT INTO usergroup
SELECT *
FROM user_group_dup;

DROP TABLE user_group_dup;

/**
  ************************************
  * USER APPLICATION
  ************************************
 */
CREATE TEMPORARY TABLE user_app_dup AS
SELECT
    user_app_counts.user_id,
    user_app_counts.application_id
FROM
    (SELECT user_id,
            application_id,
            count((user_id, application_id)) as count
     FROM userapplication
     GROUP BY user_id, application_id
     ORDER BY count((user_id, application_id)) DESC
    ) user_app_counts
WHERE user_app_counts.count > 1;


DELETE FROM userapplication
    USING user_app_dup
WHERE userapplication.user_id=user_app_dup.user_id AND userapplication.application_id=user_app_dup.application_id;

INSERT INTO userapplication
SELECT *
FROM user_app_dup;

DROP TABLE user_app_dup;

/**
 ************************************
 * GROUP APPLICATION
 ************************************
*/
CREATE TEMPORARY TABLE group_app_dup AS
SELECT
    group_app_counts.group_id,
    group_app_counts.application_id
FROM
    (SELECT group_id,
            application_id,
            count((group_id, application_id)) as count
     FROM groupapplication
     GROUP BY group_id, application_id
     ORDER BY count((group_id, application_id)) DESC
    ) group_app_counts
WHERE group_app_counts.count > 1;

DELETE FROM groupapplication
    USING group_app_dup
WHERE groupapplication.group_id=group_app_dup.group_id AND groupapplication.application_id=group_app_dup.application_id;

INSERT INTO groupapplication
SELECT *
FROM group_app_dup;

DROP TABLE group_app_dup;

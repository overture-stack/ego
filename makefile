start: 
	docker-compose -f docker-compose-all.yml  up -d
	sleep 10;
	make init-db

up:
	docker-compose -f docker-compose-all.yml  up -d

down:
	docker-compose -f docker-compose-all.yml  down

nuke:
	docker-compose -f docker-compose-all.yml  down --volumes

# needed to insert the ego ui client in ego db
init-db:
	docker exec ego_postgres_1  psql -h localhost -p 5432 -U postgres -d ego --command "INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status, errorredirecturi) VALUES ('ego ui', 'ego-ui', 'secret', 'http://localhost:8080/', '...', 'APPROVED', 'http://localhost:8080/error') on conflict do nothing"
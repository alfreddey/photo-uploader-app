.PHONY: dev up down logs build test clean image

# Start the full offline stack (app + postgres) and follow app logs.
dev: up logs

up:
	docker compose up --build -d

down:
	docker compose down

# Wipe local data volumes too (fresh DB + uploads).
clean:
	docker compose down -v

logs:
	docker compose logs -f app

# Build + run unit tests with Maven (no DB or AWS required).
test:
	mvn -B verify

# Package the fat jar locally.
build:
	mvn -B -q clean package -DskipTests

# Build just the production container image.
image:
	docker build -t photo-gallery:local .

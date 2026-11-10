.PHONY: up down logs test clean audit frontend-lock

frontend-lock:
	@if [ ! -f frontend/package-lock.json ]; then \
		echo "Generating frontend/package-lock.json..."; \
		docker run --rm -v "$(PWD)/frontend:/app" -w /app node:22.16-alpine \
			npm install --package-lock-only --ignore-scripts --no-audit --no-fund; \
	fi

up: frontend-lock
	docker compose up --build

down:
	docker compose down

logs:
	docker compose logs -f

test: frontend-lock
	docker run --rm -v "$(PWD)/backend:/app" -w /app maven:3.9.11-eclipse-temurin-21-alpine mvn -B test
	docker compose run --rm --no-deps frontend npm test

clean:
	docker compose down -v --remove-orphans

audit:
	bash scripts/release-audit.sh

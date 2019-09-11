EXECUTABLES = lein docker docker-compose npm lftp

VIRKAILIJA_DEV_CONFIG=../ataru-secrets/virkailija-local-dev.edn
HAKIJA_DEV_CONFIG=../ataru-secrets/hakija-local-dev.edn

# ----------------
# Check ataru-secrets existence and config files
# ----------------
ifeq ("$(wildcard $(VIRKAILIJA_DEV_CONFIG))","")
    $(error $(VIRKAILIJA_DEV_CONFIG) not found, clone/update ataru-secrets alongside ataru since configs are stored there)
endif

ifeq ("$(wildcard $(HAKIJA_DEV_CONFIG))","")
    $(error $(HAKIJA_DEV_CONFIG) not found, clone/update ataru-secrets alongside ataru since configs are stored there)
endif

# ----------------
# Check that all necessary tools are in path
# ----------------
check-tools:
	$(info Checking commands in path: $(EXECUTABLES) ...)
	$(foreach exec,$(EXECUTABLES),\
		$(if $(shell which $(exec)),$(info .. $(exec) found),$(error No $(exec) in PATH)))

# ----------------
# Docker build
# ----------------
build-docker-images: check-tools
	docker-compose build

install-node-modules: check-tools
	npm install

# ----------------
# Start apps
# ----------------
start-pm2: build-docker-images
	npx pm2 start pm2.config.js

start-hakija-frontend-compilation: install-node-modules
	pm2 start "Ataru Hakija frontend compilation"

start-virkailija-frontend-compilation: install-node-modules
	pm2 start "Ataru Virkailija frontend compilation"

start-watch: install-node-modules start-hakija-frontend-compilation start-virkailija-frontend-compilation
	pm2 start "Ataru Figwheel" "Ataru CSS compilation"

start-docker: build-docker-images
	pm2 start "Ataru docker images"

start-hakija: start-hakija-frontend-compilation start-docker
	pm2 start "Ataru Hakija backend (8351)"

start-virkailija: start-virkailija-frontend-compilation start-docker
	pm2 start "Ataru Virkailija backend (8350)"

# ----------------
# Stop apps
# ----------------
stop-pm2: install-node-modules
	npx pm2 stop pm2.config.js

stop-hakija-frontend-compilation:
	pm2 stop "Ataru Hakija frontend compilation"

stop-virkailija-frontend-compilation:
	pm2 stop "Ataru Virkailija frontend compilation"

stop-watch: stop-hakija-frontend-compilation stop-virkailija-frontend-compilation
	pm2 stop "Ataru Figwheel" "Ataru CSS compilation"

stop-docker:
	pm2 stop "Ataru docker images"

stop-hakija:
	pm2 stop "Ataru Hakija backend (8351)"

stop-virkailija:
	pm2 stop "Ataru Virkailija backend (8350)"

# ----------------
# Restart apps
# ----------------
restart-hakija: start-hakija

restart-virkailija: start-virkailija

restart-docker: start-docker

restart-watch: start-watch

# ----------------
# Clean commands
# ----------------
clean-docker:
	docker system prune -f

clean-lein:
	lein clean

# ----------------
# Top-level commands (all apps)
# ----------------
start: start-pm2

stop: stop-pm2

restart: stop-pm2 start-pm2

clean: stop clean-lein clean-docker
	rm -rf node_modules
	rm *.log

status:
	pm2 status

# ----------------
# Kill PM2 and all apps managed by it (= everything)
# ----------------
kill: stop-pm2
	npx pm2 kill


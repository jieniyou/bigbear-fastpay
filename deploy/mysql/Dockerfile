FROM mysql:8.0

COPY fastpay-server/src/main/resources/db/init.sql /docker-entrypoint-initdb.d/01-init.sql
COPY fastpay-server/src/main/resources/db/002_fix_client_ip.sql /docker-entrypoint-initdb.d/02-fix-client-ip.sql
COPY fastpay-server/src/main/resources/db/003_mail_event_templates.sql /docker-entrypoint-initdb.d/03-mail-event-templates.sql

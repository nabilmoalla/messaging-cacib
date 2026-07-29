FROM icr.io/ibm-messaging/mq:9.4.0.0-r3

# Baked in at build time (rather than bind-mounted) because mounting a host
# directory directly over /etc/mqm at runtime replaces the whole directory —
# including the image's own default config templates — instead of merging
# into it, which breaks the queue manager's startup scripts.
COPY 20-configure-queues.mqsc /etc/mqm/

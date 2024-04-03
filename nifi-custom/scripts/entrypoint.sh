#! /bin/sh
# VOLUME 디렉토리들의 소유자를 nifi:nifi로 변경한다.
chown nifi:nifi ${NIFI_HOME}/conf
chown nifi:nifi ${NIFI_HOME}/database_repository
chown nifi:nifi ${NIFI_HOME}/flowfile_repository
chown nifi:nifi ${NIFI_HOME}/content_repository
chown nifi:nifi ${NIFI_HOME}/provenance_repository
chown nifi:nifi ${NIFI_HOME}/state

# nifi 유저로 변경 및 ../scripts/start.sh를 실행한다.
su nifi -c "${NIFI_BASE_DIR}/scripts/start.sh"
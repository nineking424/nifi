FROM apache/nifi:1.23.1
# NIFI_HOME
COPY ./conf/nifi.properties ${NIFI_HOME}/conf/nifi.properties
COPY ./lib/*.jar ${NIFI_HOME}/lib/
# 빌드 시점에서는 VOLUME 설정된 경로들의 소유자를 변경하거나 삭제할 수 없다.(Mount 된 경로로 취급하므로)
# 실행 시점에서는 해당 경로들의 소유자를 변경할 수 있다.(삭제는 같은 이유로 불가능)
# 따라서, entrypoint.sh 에서 실행 시점에 필요한 작업을 수행 후 유저를 nifi로 변경한다.
USER root
RUN usermod -u 500 nifi && groupmod -g 533 nifi && chown -R -h nifi:nifi ${NIFI_BASE_DIR}
# NIFI_BASE_DIR
COPY ./scripts/entrypoint.sh ${NIFI_BASE_DIR}/scripts/
RUN chmod +x ${NIFI_BASE_DIR}/scripts/*.sh
ENTRYPOINT ["../scripts/entrypoint.sh"]
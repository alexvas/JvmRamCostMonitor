import jpype
from typing import Optional, Dict, Any


class JmxBeanFactory:
    """Фабрика для создания JMX bean для подключения к JVM по PID"""
    
    _jvm_started = False
    _memory_mxbeans: Dict[int, Any] = {}  # pid -> MemoryMXBean
    _jmx_connectors: Dict[int, Any] = {}  # pid -> JMXConnector (для управления жизненным циклом)
    
    @classmethod
    def _ensure_jvm(cls) -> None:
        """Убедиться, что JVM запущена для jpype"""
        if not cls._jvm_started:
            try:
                if not jpype.isJVMStarted():
                    jpype.startJVM(jpype.getDefaultJVMPath())
                cls._jvm_started = True
            except Exception:
                # JVM уже запущена или ошибка
                pass
    
    
    @classmethod
    def get_memory_mxbean(cls, pid: int) -> Optional[Any]:
        """
        Получить MemoryMXBean для JVM процесса с указанным PID
        
        Args:
            pid: Process ID целевой JVM
            
        Returns:
            MemoryMXBean или None в случае ошибки
        """
        # Возвращаем кэшированный bean, если он есть
        if pid in cls._memory_mxbeans:
            return cls._memory_mxbeans[pid]
        
        cls._ensure_jvm()
        
        try:
            # Импортируем необходимые классы
            VirtualMachine = jpype.JClass('com.sun.tools.attach.VirtualMachine')
            JMXConnectorFactory = jpype.JClass('javax.management.remote.JMXConnectorFactory')
            JMXServiceURL = jpype.JClass('javax.management.remote.JMXServiceURL')
            ManagementFactory = jpype.JClass('java.lang.management.ManagementFactory')
            
            # Подключаемся к целевой JVM
            vm = VirtualMachine.attach(str(pid))
            
            try:
                # Получаем свойства агента
                agent_properties = vm.getAgentProperties()
                connector_address = agent_properties.getProperty('com.sun.management.jmxremote.localConnectorAddress')
                
                # Если JMX агент не запущен, запускаем его
                if connector_address is None:
                    vm.startLocalManagementAgent()
                    agent_properties = vm.getAgentProperties()
                    connector_address = agent_properties.getProperty('com.sun.management.jmxremote.localConnectorAddress')
                
                if connector_address is None:
                    return None
                
                # Подключаемся к JMX коннектору
                service_url = JMXServiceURL(connector_address)
                jmx_connector = JMXConnectorFactory.connect(service_url, None)
                mbsc = jmx_connector.getMBeanServerConnection()
                
                # Получаем MemoryMXBean через MBeanServerConnection
                memory_mxbean = ManagementFactory.newPlatformMXBeanProxy(
                    mbsc,
                    'java.lang:type=Memory',
                    jpype.JClass('java.lang.management.MemoryMXBean')
                )
                
                # Кэшируем bean и коннектор
                cls._memory_mxbeans[pid] = memory_mxbean
                cls._jmx_connectors[pid] = jmx_connector
                
                return memory_mxbean
                
            finally:
                # Отключаемся от виртуальной машины (но оставляем JMX коннектор открытым)
                vm.detach()
                
        except Exception:
            return None
    
    @classmethod
    def disconnect(cls, pid: int) -> None:
        """Закрыть соединение с JVM процесса"""
        if pid in cls._jmx_connectors:
            try:
                cls._jmx_connectors[pid].close()
            except Exception:
                pass
            del cls._jmx_connectors[pid]
        
        if pid in cls._memory_mxbeans:
            del cls._memory_mxbeans[pid]

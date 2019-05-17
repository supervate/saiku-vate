saiku部署需要注意的地方:

路径(主要是注意一下元数据的存储位置就可以了，如果要做saiku集群，到时候可以考虑用weblogic存储元数据) + saiku平台库的数据库(非元数据的库)

saiku-beans.xml 文件中有两个 saiku-beans.properties文件 分别对应生产(pro)和开发(dev)模式 这个一定要注意 在不同环境记得切换!!!!

需要设置数据源配置的地方有:
    1.applicationContext-spring-security-jdbc.properties
	2.saiku-beans-dev(pro).properties
	3.web.xml:  
	    <context-param>
    		<param-name>db.url</param-name>
    		<param-value>jdbc:mysql://gzraqsoft.mynetgear.com:3306/saiku</param-value>
    	</context-param>
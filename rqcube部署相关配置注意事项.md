rqcube部署需要注意的地方:

主要是注意一下元数据的存储位置就可以了，如果要做集群，到时候可以考虑用weblogic存储元数据 + rqcube平台库的数据库(非元数据的库)

需要配置的地方:

1. saiku-beans.xml 文件中有两个 saiku-beans.properties文件 分别对应生产(pro)和开发(dev)模式 这个一定要注意 在不同环境记得切换!!!!
2.设置平台库数据源配置(非数据源等元数据，也非样例数据，而是用户库的数据源):
  rqcubeDbInfo.properties
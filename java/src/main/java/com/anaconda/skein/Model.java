package com.anaconda.skein;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.records.ApplicationAccessType;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.NodeId;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.client.api.AMRMClient.ContainerRequest;
import org.apache.hadoop.yarn.webapp.util.WebAppUtils;
import org.apache.log4j.Level;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Model {
  private static void throwIfNull(Object obj, String param)
      throws IllegalArgumentException {
    if (obj == null) {
      throw new IllegalArgumentException(param + " must be non-null");
    }
  }

  private static void throwIfLessThan(int i, int min, String param)
      throws IllegalArgumentException {
    if (i < min) {
      throw new IllegalArgumentException(param + " must be > " + min + ", got " + i);
    }
  }

  public static class Service {
    private int instances;
    private String nodeLabel;
    private int maxRestarts;
    private Resource resources;
    private Map<String, LocalResource> localResources;
    private Map<String, String> env;
    private List<String> commands;
    private Set<String> depends;

    public Service() {}

    public Service(int instances,
                   String nodeLabel,
                   int maxRestarts,
                   Resource resources,
                   Map<String, LocalResource> localResources,
                   Map<String, String> env,
                   List<String> commands,
                   Set<String> depends) {
      this.instances = instances;
      this.nodeLabel = nodeLabel;
      this.maxRestarts = maxRestarts;
      this.resources = resources;
      this.localResources = localResources;
      this.env = env;
      this.commands = commands;
      this.depends = depends;
    }

    public String toString() {
      return ("Service:\n"
              + "instances: " + instances + "\n"
              + "nodeLabel: " + nodeLabel + "\n"
              + "maxRestarts: " + maxRestarts + "\n"
              + "resources: " + resources + "\n"
              + "localResources: " + localResources + "\n"
              + "env: " + env + "\n"
              + "commands: " + commands + "\n"
              + "depends: " + depends);
    }

    public void setInstances(int instances) { this.instances = instances; }
    public int getInstances() { return instances; }

    public void setNodeLabel(String nodeLabel) { this.nodeLabel = nodeLabel; }
    public String getNodeLabel() { return nodeLabel; }

    public void setMaxRestarts(int maxRestarts) { this.maxRestarts = maxRestarts; }
    public int getMaxRestarts() { return maxRestarts; }

    public void setResources(Resource resources) { this.resources = resources; }
    public Resource getResources() { return resources; }

    public void setLocalResources(Map<String, LocalResource> r) { this.localResources = r; }
    public Map<String, LocalResource> getLocalResources() { return localResources; }

    public void setEnv(Map<String, String> env) { this.env = env; }
    public Map<String, String> getEnv() { return env; }

    public void setCommands(List<String> commands) { this.commands = commands; }
    public List<String> getCommands() { return commands; }

    public void setDepends(Set<String> depends) { this.depends = depends; }
    public Set<String> getDepends() { return depends; }

    public void validate() throws IllegalArgumentException {
      throwIfLessThan(instances, 0, "instances");
      throwIfLessThan(instances, -1, "maxRestarts");
      throwIfNull(resources, "resources");
      throwIfLessThan(resources.getMemory(), 1, "resources.memory");
      throwIfLessThan(resources.getVirtualCores(), 1, "resources.vcores");
      throwIfNull(localResources, "localResources");
      throwIfNull(env, "env");
      throwIfNull(commands, "commands");
      if (commands.size() == 0) {
        throw new IllegalArgumentException("There must be at least one command");
      }
      throwIfNull(depends, "depends");
    }
  }

  public static class Acls {
    private boolean enable;
    private List<String> viewUsers;
    private List<String> viewGroups;
    private List<String> modifyUsers;
    private List<String> modifyGroups;
    private List<String> uiUsers;

    public Acls(boolean enable, List<String> viewUsers,
                List<String> viewGroups, List<String> modifyUsers,
                List<String> modifyGroups, List<String> uiUsers) {
      this.enable = enable;
      this.viewUsers = viewUsers;
      this.viewGroups = viewGroups;
      this.modifyUsers = modifyUsers;
      this.modifyGroups = modifyGroups;
      this.uiUsers = uiUsers;
    }

    public Map<ApplicationAccessType, String> getYarnAcls() {
      if (!enable) {
        return null;
      }
      Map<ApplicationAccessType, String> out = new HashMap<ApplicationAccessType, String>();

      out.put(ApplicationAccessType.VIEW_APP, Utils.formatAcl(viewUsers, viewGroups));
      out.put(ApplicationAccessType.MODIFY_APP, Utils.formatAcl(modifyUsers, modifyGroups));

      return out;
    }

    public void setEnable(boolean enable) { this.enable = enable; }
    public boolean getEnable() { return enable; }

    public void setViewUsers(List<String> viewUsers) { this.viewUsers = viewUsers; }
    public List<String> getViewUsers() { return viewUsers; }

    public void setViewGroups(List<String> viewGroups) { this.viewGroups = viewGroups; }
    public List<String> getViewGroups() { return viewGroups; }

    public void setModifyUsers(List<String> modifyUsers) { this.modifyUsers = modifyUsers; }
    public List<String> getModifyUsers() { return modifyUsers; }

    public void setModifyGroups(List<String> modifyGroups) { this.modifyGroups = modifyGroups; }
    public List<String> getModifyGroups() { return modifyGroups; }

    public void setUiUsers(List<String> uiUsers) { this.uiUsers = uiUsers; }
    public List<String> getUiUsers() { return uiUsers; }
  }

  public static class Master {
    private LocalResource logConfig;
    private Level logLevel;

    public Master(LocalResource logConfig, Level logLevel) {
      this.logConfig = logConfig;
      this.logLevel = logLevel;
    }

    public void setLogConfig(LocalResource logConfig) { this.logConfig = logConfig; }
    public LocalResource getLogConfig() { return this.logConfig; }
    public boolean hasLogConfig() { return this.logConfig != null; }

    public void setLogLevel(Level logLevel) { this.logLevel = logLevel; }
    public Level getLogLevel() { return this.logLevel; }
  }

  public static class ApplicationSpec {
    private String name;
    private String queue;
    private String nodeLabel;
    private int maxAttempts;
    private Set<String> tags;
    private List<Path> fileSystems;
    private Acls acls;
    private Master master;
    private Map<String, Service> services;

    public ApplicationSpec() {}

    public ApplicationSpec(String name, String queue, String nodeLabel, int maxAttempts,
                           Set<String> tags, List<Path> fileSystems,
                           Acls acls, Master master,
                           Map<String, Service> services) {
      this.name = name;
      this.queue = queue;
      this.nodeLabel = nodeLabel;
      this.maxAttempts = maxAttempts;
      this.tags = tags;
      this.fileSystems = fileSystems;
      this.acls = acls;
      this.master = master;
      this.services = services;
    }

    public String toString() {
      return ("ApplicationSpec<"
              + "name: " + name + ", "
              + "queue: " + queue + ", "
              + "nodeLabel: " + nodeLabel + ", "
              + "maxAttempts: " + maxAttempts + ", "
              + "tags: " + tags + ", "
              + "fileSystems" + fileSystems + ", "
              + "services: " + services + ">");
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setQueue(String queue) { this.queue = queue; }
    public String getQueue() { return queue; }

    public void setNodeLabel(String nodeLabel) { this.nodeLabel = nodeLabel; }
    public String getNodeLabel() { return nodeLabel; }

    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getMaxAttempts() { return maxAttempts; }

    public void setTags(Set<String> tags) { this.tags = tags; }
    public Set<String> getTags() { return this.tags; }

    public void setFileSystems(List<Path> fileSystems) {
      this.fileSystems = fileSystems;
    }
    public List<Path> getFileSystems() { return this.fileSystems; }

    public void setAcls(Acls acls) { this.acls = acls; }
    public Acls getAcls() { return this.acls; }

    public void setMaster(Master master) { this.master = master; }
    public Master getMaster() { return this.master; }

    public void setServices(Map<String, Service> services) { this.services = services; }
    public Map<String, Service> getServices() { return services; }

    public void validate() throws IllegalArgumentException {
      throwIfNull(name, "name");
      throwIfNull(queue, "queue");
      throwIfLessThan(maxAttempts, 1, "maxAttempts");
      throwIfNull(tags, "tags");
      throwIfNull(fileSystems, "fileSystems");
      throwIfNull(services, "services");
      if (services.size() == 0) {
        throw new IllegalArgumentException("There must be at least one service");
      }
      for (Service s: services.values()) {
        s.validate();
      }
    }
  }

  public static class Container {
    public enum State {
      WAITING,
      REQUESTED,
      RUNNING,
      SUCCEEDED,
      FAILED,
      KILLED
    }

    private String serviceName;
    private int instance;
    private State state;
    private ContainerId yarnContainerId;
    private NodeId yarnNodeId;
    private String yarnNodeHttpAddress;
    private long startTime;
    private long finishTime;
    private ContainerRequest req;
    private Set<String> ownedKeys;
    private String exitMessage;

    public Container() {}

    public Container(String serviceName, int instance, State state) {
      this.serviceName = serviceName;
      this.instance = instance;
      this.state = state;
      this.yarnContainerId = null;
      this.startTime = 0;
      this.finishTime = 0;
      this.ownedKeys = new HashSet<String>();
    }

    public String toString() {
      return ("Container<"
              + "serviceName: " + serviceName + ", "
              + "instance: " + instance + ">");
    }

    public String getId() { return serviceName + "_" + instance; }

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getServiceName() { return serviceName; }

    public void setInstance(int instance) { this.instance = instance; }
    public int getInstance() { return instance; }

    public void setState(State state) { this.state = state; }
    public State getState() { return state; }

    public boolean completed() {
      switch (state) {
        case WAITING:
        case REQUESTED:
        case RUNNING:
          return false;
        default:
          return true;
      }
    }

    public void setYarnContainerId(ContainerId yarnContainerId) {
      this.yarnContainerId = yarnContainerId;
    }
    public ContainerId getYarnContainerId() { return yarnContainerId; }

    public void setYarnNodeId(NodeId yarnNodeId) { this.yarnNodeId = yarnNodeId; }
    public NodeId getYarnNodeId() { return yarnNodeId; }

    public void setYarnNodeHttpAddress(String yarnNodeHttpAddress) {
      this.yarnNodeHttpAddress = yarnNodeHttpAddress;
    }
    public String getYarnNodeHttpAddress() { return yarnNodeHttpAddress; }

    public String getLogsAddress() {
      if (yarnNodeHttpAddress == null || yarnContainerId == null) {
        return "";  // Not able to construct a URL yet.
      }

      return WebAppUtils.getRunningLogURL(
          yarnNodeHttpAddress,
          yarnContainerId.toString(),
          System.getenv(Environment.USER.name())
      );
    }

    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getStartTime() { return startTime; }

    public void setFinishTime(long finishTime) { this.finishTime = finishTime; }
    public long getFinishTime() { return finishTime; }

    public void setExitMessage(String diagnostics) { this.exitMessage = diagnostics; }
    public String getExitMessage() { return exitMessage; }

    public void setContainerRequest(ContainerRequest req) { this.req = req; }
    public ContainerRequest popContainerRequest() {
      ContainerRequest out = this.req;
      this.req = null;
      return out;
    }

    public void addOwnedKey(String key) {
      ownedKeys.add(key);
    }

    public void removeOwnedKey(String key) {
      ownedKeys.remove(key);
    }

    public Set<String> getOwnedKeys() { return ownedKeys; }
    public void clearOwnedKeys() { ownedKeys.clear(); }
  }
}

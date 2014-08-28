package org.apache.itest.util

import com.cloudera.api.ClouderaManagerClientBuilder
import com.cloudera.api.DataView
import com.cloudera.api.model.ApiCluster
import com.cloudera.api.model.ApiClusterList
import com.cloudera.api.model.ApiHost
import com.cloudera.api.model.ApiHostList
import com.cloudera.api.model.ApiRole
import com.cloudera.api.model.ApiRoleList
import com.cloudera.api.model.ApiRoleNameList
import com.cloudera.api.model.ApiRoleState
import com.cloudera.api.model.ApiService
import com.cloudera.api.model.ApiServiceList
import com.cloudera.api.v1.RoleCommandsResource
import com.cloudera.api.v7.RootResourceV7
import org.apache.bigtop.itest.Contract
import org.apache.bigtop.itest.ParameterSetter
import org.apache.bigtop.itest.Property
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory


@Contract(
    properties =
    [@Property(name="cm.username", type=Property.Type.STRING, defaultValue="admin"),
     @Property(name="cm.password", type=Property.Type.STRING, defaultValue="admin"),
     @Property(name="cm.host", type=Property.Type.STRING, defaultValue="localhost"),
     @Property(name="cm.port", type=Property.Type.INT, intValue=7180),
     @Property(name="cm.cluster", type=Property.Type.STRING, defaultValue="cluster")],
    env = [])
class CMClient {
    static private Log LOG = LogFactory.getLog(CMClient.class);

    private static String cm_username;
    private static String cm_password;
    private static String cm_host;
    private static int cm_port;
    private static String cm_cluster;

    static{
        ParameterSetter.setProperties(CMClient.class);
        LOG.info("CM server: ${cm_host}");

    }

    private RootResourceV7 apiRoot;

    public CMClient(){

        apiRoot = new ClouderaManagerClientBuilder()
                .withHost(cm_host).withPort(cm_port)
                .withUsernamePassword(cm_username, cm_password)
                .build()
                .getRootV7();

        // Get a list of defined clusters
        ApiClusterList clusters = apiRoot.getClustersResource()
                .readClusters(DataView.SUMMARY);
        boolean found = false;
        for(ApiCluster cluster : clusters.getClusters()){
            if(cluster.getName().equals(cm_cluster)){
                found = true;
            }
        }
        if(!found){
            LOG.error("Cluster ${cm_cluster} is not found");
        }
    }

    public ApiHost findHostById(String hostId){
        ApiHostList hosts = apiRoot.getHostsResource().readHosts(DataView.SUMMARY);
        for(ApiHost h : hosts){
            if(h.getHostId().equals(hostId))
                return h;
        }
        return null;
    }

    private ApiServiceList getServices(){
        return apiRoot.getClustersResource().getServicesResource(cm_cluster).readServices(DataView.SUMMARY);
    }

    private ApiRoleList getRoles(String service){
        for(ApiService s : getServices()){
            if(s.getName().equals(service)){
                return apiRoot.getClustersResource().getServicesResource(cm_cluster).getRolesResource(service).readRoles();
            }
        }
        return null;
    }

    private ApiRoleState getRoleState(String role){
        for(ApiService s : getServices()){
            for(ApiRole r : getRoles(s.getName())){
                if(r.getName().equals(role)){
                    return r.getRoleState();
                }
            }
        }
        return null;
    }


    public boolean checkRoleState(String role, boolean started){
        ApiRoleState state = getRoleState(role);
        if(state != null)
            return state.equals(started ? ApiRoleState.STARTED : ApiRoleState.STOPPED);
        else
            return false;
    }

    public List<ApiRole> findRoles(String service, String rolePattern){
        def roles = new ArrayList<ApiRole>();
        for(ApiService s :  getServices()){
            if(s.getName().equals(service)){
                for(ApiRole r : getRoles(s.getName())){
                    if(r.getName().contains(rolePattern)){
                        roles.add(r);
                    }
                }
            }
        }
        return roles;
    }

    public String findRoleByHost(String node, String service, String rolePattern){
        for(ApiService s :  getServices()){
           if(s.getName().equals(service)){
               for(ApiRole r : getRoles(s.getName())){
                    if(r.getName().contains(rolePattern)){
                        ApiHost host = findHostById(r.getHostRef().getHostId());
                        if(host != null && (
                           host.getHostname().contains(node) ||
                           host.getIpAddress().equals(node)))
                            return r.getName();
                    }
               }
           }
        }
        return null;
    }

    public void startProcess(String role){
        for(ApiService s : getServices()){
            for(ApiRole r : getRoles(s.getName())){
                if(r.getName().contains(role)){
                    RoleCommandsResource cr = apiRoot.getClustersResource().getServicesResource(cm_cluster).getRoleCommandsResource(s.getName());
                    ApiRoleNameList rl = new ApiRoleNameList();
                    rl.add(r.getName());
                    cr.startCommand(rl);
                }
            }
        }
    }

    public boolean waitForProcess(String role, boolean started, int timeout){
        int elapse = 0;
        LOG.info("Waiting for role ${role} " + (started ? "started" : "stopped"));
        while(elapse < timeout){
            ApiRoleState state = getRoleState(role);
            if(state.equals(started ? ApiRoleState.STARTED : ApiRoleState.STOPPED))
                return true;
            Thread.sleep(5000);
            elapse += 5;
        }
        return false;
    }

    public void stopProcess(String role){
        for(ApiService s : getServices()){
            for(ApiRole r : getRoles(s.getName())){
                if(r.getName().contains(role)){
                    RoleCommandsResource cr = apiRoot.getClustersResource().getServicesResource(cm_cluster).getRoleCommandsResource(s.getName());
                    ApiRoleNameList rl = new ApiRoleNameList();
                    rl.add(r.getName());
                    cr.stopCommand(rl);
                }
            }
        }
    }
}

/*
 *   Copyright 2015 OSBI Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.saiku.web.rest.resources;

import clover.org.jfree.chart.LegendItemSource;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.vate.EncryptAndDecryptUtil;
import org.apache.commons.lang.StringUtils;
import org.saiku.database.dto.MondrianSchema;
import org.saiku.database.dto.SaikuUser;
import org.saiku.datasources.datasource.SaikuDatasource;
import org.saiku.log.LogExtractor;
import org.saiku.service.datasource.DatasourceService;
import org.saiku.service.datasource.IDatasourceManager;
import org.saiku.service.olap.OlapDiscoverService;
import org.saiku.service.user.UserService;
import org.saiku.service.util.exception.SaikuDataSourceException;
import org.saiku.service.util.exception.SaikuServiceException;
import org.saiku.web.rest.objects.DataSourceMapper;

import com.qmino.miredot.annotations.ReturnType;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import org.apache.commons.io.IOUtils;
import org.saiku.service.importer.JujuSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

import javax.jcr.RepositoryException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

/**
 * AdminResource for the Saiku 3.0+ Admin console
 */
@Component
@Path("/saiku/admin")
public class AdminResource {

    private DatasourceService datasourceService;

    private UserService userService;
    private static final Logger log = LoggerFactory.getLogger(DataSourceResource.class);
    private OlapDiscoverService olapDiscoverService;
    private LogExtractor logExtractor;
    private EncryptAndDecryptUtil encryptAndDecryptUtil;

    private static final String accessAccount = "rqpanda";
    private static final String accessPwd = "eLXZYVEg292bgbD6";

    public LogExtractor getLogExtractor() {
        return logExtractor;
    }

    public void setLogExtractor(LogExtractor logExtractor) {
        this.logExtractor = logExtractor;
    }

    public void setOlapDiscoverService(OlapDiscoverService olapDiscoverService) {
        this.olapDiscoverService = olapDiscoverService;
    }

    public void setDatasourceService(DatasourceService ds) {
        datasourceService = ds;
    }

    public void setUserService(UserService us) {
        userService = us;
    }


    private IDatasourceManager repositoryDatasourceManager;

    public IDatasourceManager getRepositoryDatasourceManager() {
        return repositoryDatasourceManager;
    }

    public void setRepositoryDatasourceManager(
        IDatasourceManager repositoryDatasourceManager) {
        this.repositoryDatasourceManager = repositoryDatasourceManager;
    }
    /**
     * Get all the available data sources on the platform.
     * @return A response containing a list of datasources.
     * @summary Get Saiku Datasources
     */
    @GET
    @Produces( {"application/json"})
    @Path("/datasources")
    @ReturnType("java.lang.List<SaikuDatasource>")
    public Response getAvailableDataSources() {
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        
        List<DataSourceMapper> l = new ArrayList<>();
        
        try {
            for (SaikuDatasource d : datasourceService.getDatasources(userService.getCurrentUserRoles()).values()) {
                l.add(new DataSourceMapper(d));
            }
            return Response.ok().entity(l).build();
        } catch (SaikuServiceException e) {
            log.error(this.getClass().getName(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).type("text/plain").build();
        }
    }

    /**
     * Update a specific Saiku data source.
     * @summary Update data source
     * @param json The Json data source object
     * @param id The datasource id.
     * @return A response containing the datasource.
     */
    @PUT
    @Produces( {"application/json"})
    @Consumes( {"application/json"})
    @Path("/datasources/{id}")
    @ReturnType("org.saiku.web.rest.objects.DataSourceMapper")
    public Response updateDatasource(DataSourceMapper json, @PathParam("id") String id) {
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            datasourceService.addDatasource(json.toSaikuDataSource(), true, userService.getCurrentUserRoles());
            return Response.ok().type("application/json").entity(json).build();
        } catch (Exception e){
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage())
                           .type("text/plain").build();
        }
    }

    /**
     * Refresh a Saiku data source.
     * @summary Refresh data source
     * @param id The data source id.
     * @return A response containing the data source definition.
     */
    @GET
    @Produces( {"application/json"})
    @Path("/datasources/{id}/refresh")
    @ReturnType("java.util.List<SaikuConnection>")
    public Response refreshDatasource(@PathParam("id") String id) {
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            olapDiscoverService.refreshConnection(id);
            return Response.ok().entity(olapDiscoverService.getConnection(id)).type("application/json").build();
        } catch (Exception e) {
            log.error(this.getClass().getName(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage())
                           .type("text/plain").build();
        }

    }

    /**
     * Create a data source on the Saiku server.
     * @summary Create data source
     * @param json The json data source object
     * @return A response containing the data source object
     */
    @POST
    @Produces( {"application/json"})
    @Consumes( {"application/json"})
    @Path("/datasources")
    @ReturnType("org.saiku.web.rest.objects.DataSourceMapper")
    public Response createDatasource(DataSourceMapper json) {
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            datasourceService.addDatasource(json.toSaikuDataSource(), false, userService.getCurrentUserRoles());
            return Response.ok().entity(json).type("application/json").build();
        } catch (Exception e) {
            log.error("Error adding data source", e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(e.getLocalizedMessage())
                           .type("text/plain").build();
        }
    }

    /**
     * Delete data source from the Saiku server
     * @summary Delete data source
     * @param id The data source ID
     * @return A response containing a list of data sources remaining on the platform.
     */
    @DELETE
    @Path("/datasources/{id}")
    public Response deleteDatasource(@PathParam("id") String id) {
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        
        datasourceService.removeDatasource(id);
        
        return Response.ok().type("application/json")
            .entity(datasourceService.getDatasources(userService.getCurrentUserRoles())).build();
    }

    /**
     * Get all the available schema.
     * @summary Get Saiku schema.
     * @return A list of schema
     */
    @GET
    @Produces( {"application/json"})
    @Path("/schema")
    @ReturnType("java.util.List<MondrianSchema>")
    public Response getAvailableSchema() {

        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok().entity(datasourceService.getAvailableSchema()).build();
    }

    /**
     * Upload a new schema to the Saiku server.
     * @summary Upload schema
     * @param is Input stream (file form data param)
     * @param detail Detail (file form data param)
     * @param name Schema name
     * @param id Schema id
     * @return A response containing a list of available schema.
     */
    @PUT
    @Produces( {"application/json"})
    @Consumes("multipart/form-data")
    @Path("/schema/{id}")
    @ReturnType("java.util.List<MondrianSchema>")
    public Response uploadSchemaPut(@FormDataParam("file") InputStream is, @FormDataParam("file") FormDataContentDisposition detail,
                                    @FormDataParam("name") String name, @PathParam("id") String id) {
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        String path = "/datasources/" + name + ".xml";
        String schema = getStringFromInputStream(is);
        try {
            datasourceService.addSchema(schema, path, name);
            return Response.ok().entity(datasourceService.getAvailableSchema()).build();
        } catch (Exception e) {
            log.error("Error uploading schema: "+name, e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(e.getLocalizedMessage())
                           .type("text/plain").build();
        }

    }

    /**
     * Upload new schema to the Saiku server
     * @summary Upload new schema
     * @summary Upload schema
     * @param is Input stream (file form data param)
     * @param detail Detail (file form data param)
     * @param name Schema name
     * @param id Schema id
     * @return A response containing a list of available schema.
     */
    @POST
    @Produces( {"application/json"})
    @Consumes("multipart/form-data")
    @Path("/schema/{id}")
    @ReturnType("java.util.List<MondrianSchema>")
    public Response uploadSchema(@FormDataParam("file") InputStream is, @FormDataParam("file") FormDataContentDisposition detail,
                                 @FormDataParam("name") String name, @PathParam("id") String id) {
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        String path = "/datasources/" + name + ".xml";
        String schema = getStringFromInputStream(is);
        try {
            datasourceService.addSchema(schema, path, name);
            return Response.ok().entity(datasourceService.getAvailableSchema()).build();
        } catch (Exception e) {
            log.error("Error uploading schema: "+name, e);
            return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR)
                           .entity(e.getLocalizedMessage())
                           .type("text/plain").build();
        }

    }

    /**
     * Updates the locale parameter of the datasource
     * @param locale: the new locale for the data source
     * @param datasourceName: ID of the data source whose locale should be changed
     * @return: Response indicating success or fail
     */
    @PUT
    @Produces({"application/json"})
    @Consumes({"application/json"})
    @Path("/datasources/{datasourceName}/locale")
    @ReturnType("org.saiku.web.rest.objects.DataSourceMapper")
    public Response updateDatasourceLocale(String locale, @PathParam("datasourceName") String datasourceName) {
        try {
            boolean overwrite = true;
            SaikuDatasource saikuDatasource = datasourceService.getDatasource(datasourceName);
            datasourceService.setLocaleOfDataSource(saikuDatasource, locale);
            datasourceService.addDatasource(saikuDatasource, overwrite, userService.getCurrentUserRoles());
            return Response.ok().type("application/json").entity(new DataSourceMapper(saikuDatasource)).build();
        } catch(SaikuDataSourceException e){
            return Response.ok().type("application/json").entity(e.getLocalizedMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage())
                .type("text/plain").build();
        }
    }

    /**
     * Get existing Saiku users from the Saiku server.
     * @summary Get Saiku users.
     * @return A list of available users.
     */
    @GET
    @Produces( {"application/json"})
    @Path("/users")
    @ReturnType("java.util.List<SaikuUser>")
    public Response getExistingUsers(@QueryParam("actk") String actk) {
        if(!userService.isAdmin() && !isAccessAble(actk)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok().entity(userService.getUsers()).build();
    }

    /**
     * Get existing Saiku users from the Saiku server.
     * @summary Get Saiku users.
     * @return A list of available users.
     */
    @POST
    @Produces( {"application/json"})
    @Path("/allUsers")
    @ReturnType("java.util.List<SaikuUser>")
    public Response getExistingUsersPost(@FormParam("actk") String actk) {
        if(!userService.isAdmin() && !isAccessAble(actk)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok().entity(userService.getUsers()).build();
    }

    /**
     * Delete a schema from the Saiku server.
     * @summary Delete a schema.
     * @param id The schema ID.
     * @return A response containing available schema.
     */
    @DELETE
    @Path("/schema/{id}")
    @ReturnType("java.util.List<MondrianSchema>")
    public Response deleteSchema(@PathParam("id") String id) {
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        datasourceService.removeSchema(id);
        return Response.status(Response.Status.NO_CONTENT).entity(datasourceService.getAvailableSchema()).build();
    }

    /**
     * Get Saved Schema By ID
     * @param id
     * @return a schema file.
     */
    @GET
    @Path("/schema/{id}")
    @Produces("application/xml")
    @ReturnType("MondrianSchema")
    public Response getSavedSchema(@PathParam("id") String id){
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        String p = "";
        for(MondrianSchema s :datasourceService.getAvailableSchema()){
            if(s.getName().equals(id)){

                try {
                    p= repositoryDatasourceManager.getInternalFileData(s.getPath());
                } catch (RepositoryException e) {
                    Response.serverError().entity(e.getLocalizedMessage()).build();
                }
                break;
            }
        }

        return Response
            .ok(p.getBytes(), MediaType.APPLICATION_OCTET_STREAM)
            .header("content-disposition", "attachment; filename = " + id)
            .build();
    }

    /**
     * Import a legacy data source into the Saiku server.
     * @summary Import legacy datasource.
     * @return A status 200.
     */
    @GET
    @Path("/datasource/import")
    public Response importLegacyDatasources() {

        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        datasourceService.importLegacyDatasources();
        return Response.ok().build();
    }

    /**
     * Import legacy schema.
     * @summary Import legacy schema
     * @return A status 200
     */
    @GET
    @Path("/schema/import")
    public Response importLegacySchema() {
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        datasourceService.importLegacySchema();
        return Response.ok().build();
    }

    /**
     * Import legacy users into the Saiku server/
     * @summary Import legacy users.
     * @return A status 200.
     */
    @GET
    @Path("/users/import")
    public Response importLegacyUsers() {

        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        datasourceService.importLegacyUsers();
        return Response.ok().build();
    }

    /**
     * Get user details for a user in the Saiku server.
     * @summary Get user details.
     * @param id The user ID.
     * @return A response containing the user details object for the selected user.
     */
    @GET
    @Produces( {"application/json"})
    @Path("/users/{id}")
    @ReturnType("org.saiku.database.dto.SaikuUser")
    public Response getUserDetails(@PathParam("id") int id,@QueryParam("actk") String actk) {
        if(!userService.isAdmin() && !isAccessAble(actk)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok().entity(userService.getUser(id)).build();
    }

    /**
     * Update a users user details on the Saiku server.
     * @summary Update user details
     * @param jsonString SaikuUser object.
     * @param userName The username for the user to be updated.
     * @return A response containing a user object.
     */
    @PUT
    @Produces( {"application/json"})
    @Consumes("application/json")
    @Path("/users/{username}")
    @ReturnType("org.saiku.database.dto.SaikuUser")
    public Response updateUserDetails(SaikuUser jsonString, @PathParam("username") String userName,@FormParam("actk") String actk) {
        if(!userService.isAdmin() && !isAccessAble(actk)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        if(jsonString.getPassword() == null || jsonString.getPassword().equals("")) {
            return Response.ok().entity(userService.updateUser(jsonString, false)).build();
        }
        else{
            return Response.ok().entity(userService.updateUser(jsonString, true)).build();
        }
    }

    /**
     * Create user details on the Saiku server.
     * @summary Create user details
     * @param jsonString SaikuUser object
     * @return A response containing the user object.
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Path("/users")
    @ReturnType("org.saiku.database.dto.SaikuUser")
    public Response createUserDetails(SaikuUser jsonString,@FormParam("actk") String actk) {

        if(!userService.isAdmin() && !isAccessAble(actk)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        return Response.ok().entity(userService.addUser(jsonString)).build();
    }

    /**
     * by vate
     * 批量添加用户接口
     * @summary Create user details
     * @param usersJsonStr SaikuUser object
     * @return 返回添加失败的用户名列表
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Path("/addUsers")
    @ReturnType("java.lang.String")
    public Response addUsers(@FormParam("usersJsonStr") String usersJsonStr,@FormParam("actk") String actk) {

        if(!userService.isAdmin() && !isAccessAble(actk)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<String> errUserNames = new ArrayList<>();
        JSONArray users = JSONUtil.parseArray(usersJsonStr);
        if (users != null && !users.isEmpty()){
            for (int i = 0; i < users.size(); i++) {
                JSONObject userObj = users.getJSONObject(i);
                SaikuUser user = new SaikuUser();
                user.setEmail("x@x.com");
                String[] roles = {"ROLE_USER"};
                if (userObj.getJSONArray("roles") != null){
                    JSONArray roleArr = userObj.getJSONArray("roles");
                    user.setRoles(roleArr.toArray(new String[roleArr.size()]));
                }else {
                    user.setRoles(roles);
                }
                user.setPassword(userObj.getStr("password"));
                user.setUsername(userObj.getStr("username"));
                SaikuUser storedUser = userService.addUser(user);
                if(storedUser.getId() == 0){
                    errUserNames.add(user.getUsername());
                }
            }
        }
        return Response.ok(errUserNames).build();
    }

    /**
     * by vate
     * 批量更新用户接口
     * 注意：每个用户都需要带上id
     * @summary Create user details
     * @param usersJsonStr SaikuUser object
     * @return 返回更新失败的用户名列表
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Path("/updateUsers")
    @ReturnType("java.lang.String")
    public Response updateUserDetails(@FormParam("usersJsonStr") String usersJsonStr,@FormParam("actk") String actk) {
        if(!userService.isAdmin() && !isAccessAble(actk)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        List<String> errUserNames = new ArrayList<>();
        JSONArray users = JSONUtil.parseArray(usersJsonStr);
        List<SaikuUser> allUsers = userService.getUsers();
        Map<String,SaikuUser> allUserMap = new HashMap<>();
        if (allUsers !=null && !allUsers.isEmpty()){
            for (SaikuUser user : allUsers) {
                allUserMap.put(user.getUsername(),user);
            }
        }
        if (users != null && !users.isEmpty()){
            for (int i = 0; i < users.size(); i++) {
                boolean updatePassword = false;
                JSONObject userObj = users.getJSONObject(i);
                String username = userObj.getStr("username");
                //用户名未传，忽略
                if (StringUtils.isBlank(username)) continue;
                if (allUserMap.get(username) == null){
                    //如果Saiku不包含该用户名，加入更新错误列表
                    errUserNames.add(username);
                    continue;
                }
                SaikuUser user = new SaikuUser();
                user.setId(allUserMap.get(username).getId());
                if (userObj.getJSONArray("roles") != null){
                    JSONArray roleArr = userObj.getJSONArray("roles");
                    user.setRoles(roleArr.toArray(new String[roleArr.size()]));
                }
                if (userObj.getStr("username") != null){
                    user.setUsername(userObj.getStr("username"));
                }
                if (userObj.getStr("password") != null) {
                    user.setPassword(userObj.getStr("password"));
                    updatePassword = true;
                }
                SaikuUser updatedUser = userService.updateUser(user,updatePassword);
            }
        }
        return Response.ok(errUserNames).build();
    }

    /**
     * by vate
     * 批量删除用户接口
     * @summary Create user details
     * @param userNames SaikuUser object
     * @return 反正就是成功。。。
     */
    @POST
    @Consumes("application/x-www-form-urlencoded")
    @Path("/delUsers")
    @ReturnType("java.lang.String")
    public Response removeUsers(@FormParam("userNames") String userNames,@FormParam("actk") String actk) {

        if(!userService.isAdmin() && !isAccessAble(actk)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        String[] userNameArr = userNames.split(",");
        if (userNameArr != null && userNameArr.length >0){
            for (int i = 0; i < userNameArr.length; i++) {
                userService.removeUser(userNameArr[i]);
            }
        }
        return Response.ok().build();
    }

    /**
     * Delete a user from the Saiku server.
     * @summary Delete user.
     * @param username The username to remove
     * @return A status 200.
     */
    @DELETE
    @Produces( {"application/json"})
    @Path("/users/{username}")
    public Response removeUser(@PathParam("username") String username,@FormParam("actk") String actk) {
        if(!userService.isAdmin() && !isAccessAble(actk)){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        userService.removeUser(username);
        return Response.ok().build();
    }

    /**
     * Get string from an input stream object.
     * @param is The input stream to convert.
     * @return A string representation of the input stream.
     */
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            log.error("IO Exception when reading from input stream", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    log.error("IO Exception closing input stream",e );
                }
            }
        }

        return sb.toString();

    }

    /**
     * Get the Saiku server version.
     * @summary Get Saiku version.
     * @return A Response containing the Saiku server version.
     */
    @GET
    @Produces("text/plain")
    @Path("/version")
    @ReturnType("java.lang.String")
    public Response getVersion(){
        Properties prop = new Properties();
        String version = "";
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream("org/saiku/web/rest/resources/version.properties");
        try {
            // load a properties file
            prop.load(is);

            // get the property value and print it out
            version = prop.getProperty("VERSION");
        } catch (IOException ex) {
            log.error("IO Exception when reading input stream", ex);
        }
        return Response.ok().entity(version).type("text/plain").build();
    }

    /**
     * Backup the Saiku server repository.
     * @summary Backup the repository.
     * @return A Zip file containing the backup.
     */
    @GET
    @Produces("application/zip")
    @Path("/backup")
    public StreamingOutput getBackup(){
        if(!userService.isAdmin()){
            return null;
        }
        return new StreamingOutput() {
            public void write(OutputStream output) throws IOException, WebApplicationException {
                BufferedOutputStream bus = new BufferedOutputStream(output);
                bus.write(datasourceService.exportRepository());

            }
        };
    }

    /**
     * Restore the repository on a Saiku server.
     * @summary Restore a backup
     * @param is The input stream
     * @param detail The file detail
     * @return A status 200.
     */
    @POST
    @Produces("text/plain")
    @Consumes("multipart/form-data")
    @Path("/restore")
    public Response postRestore(@FormDataParam("file") InputStream is, @FormDataParam("file") FormDataContentDisposition detail){
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            byte[] bytes = IOUtils.toByteArray(is);
            datasourceService.restoreRepository(bytes);
            return Response.ok().entity("Restore Ok").type("text/plain").build();
        } catch (IOException e) {
            log.error("Error reading restore file", e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Restore Ok").type("text/plain").build();
    }

    /**
     * Restore old legacy files on the Saiku server.
     * @param is Input stream
     * @param detail The file detail
     * @return A status 200
     */
    @POST
    @Produces("text/plain")
    @Consumes("multipart/form-data")
    @Path("/legacyfiles")
    public Response postRestoreFiles(@FormDataParam("file") InputStream is, @FormDataParam("file") FormDataContentDisposition detail){
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            byte[] bytes = IOUtils.toByteArray(is);
            datasourceService.restoreLegacyFiles(bytes);
            return Response.ok().entity("Restore Ok").type("text/plain").build();
        } catch (IOException e) {
            log.error("Error reading restore file", e);
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Restore Ok").type("text/plain").build();
    }

    @GET
    @Produces("text/plain")
    @Path("/log/{logname}")
    public Response getLogFile(@PathParam("logname") String logname){
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        try {
            return Response.status(Response.Status.OK).entity(logExtractor.readLog(logname)).build();
        } catch (IOException e) {
            log.error("Could not read log file",e);
            return Response.serverError().entity("Could not read log file").build();
        }
    }

    @GET
    @Produces("application/json")
    @Path("/datakeys")
    public Response getPropertiesKeys(){
        return Response.ok(repositoryDatasourceManager.getAvailablePropertiesKeys()).build();
    }

    @GET
    @Produces("application/json")
    @Path("/attacheddatasources")
    public Response getDataSources(){
        if(!userService.isAdmin()){
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        List<JujuSource> list = repositoryDatasourceManager.getJujuDatasources();


        return Response.ok(list).build();

    }

    public boolean isAccessAble(String actk) {
        //by vate
        //自定义的字段，里面包含用户名和密码信息
        if (StringUtils.isNotBlank(actk)) {
            //解析出username 和 pwd
            String actkInfo = encryptAndDecryptUtil.decryptContentForRqpanda(actk);
            String[] usernameAndPwd = actkInfo.split(",");
            if (usernameAndPwd.length >= 2) {
                String userName = usernameAndPwd[0];
                String pwd = usernameAndPwd[1];
                if (accessAccount.equals(userName) && accessPwd.equals(pwd)){
                    return true;
                }
            } else {
                log.error("actk参数内容有误！解析失败！");
            }
        }
        return false;
    }

    public EncryptAndDecryptUtil getEncryptAndDecryptUtil() {
        return encryptAndDecryptUtil;
    }

    public void setEncryptAndDecryptUtil(EncryptAndDecryptUtil encryptAndDecryptUtil) {
        this.encryptAndDecryptUtil = encryptAndDecryptUtil;
    }
}

/*  
 *   Copyright 2012 OSBI Ltd
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


import com.vate.EncryptAndDecryptUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.saiku.olap.query2.ThinQuery;
import org.saiku.service.user.UserService;
import org.saiku.web.rest.objects.resultset.QueryResult;
import org.saiku.web.rest.util.ServletUtil;
import org.saiku.web.svg.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * QueryServlet contains all the methods required when manipulating an OLAP Query.
 * @author Paul Stoellberger
 *
 */
@Component
@Path("/saiku/export")
@XmlAccessorType(XmlAccessType.NONE)
public class ExporterResourceByActk {

	private static final Logger log = LoggerFactory.getLogger(ExporterResourceByActk.class);

	private ISaikuRepository repository;

	private Query2Resource query2Resource;

	public EncryptAndDecryptUtil encryptAndDecryptUtil;

	public UserService userService;

	public void setEncryptAndDecryptUtil(EncryptAndDecryptUtil encryptAndDecryptUtil) {
		this.encryptAndDecryptUtil = encryptAndDecryptUtil;
	}

	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	public void setQuery2Resource(Query2Resource qr){
		this.query2Resource = qr;
	}

	public void setRepository(ISaikuRepository repository){
		this.repository = repository;
	}

  /**
   * Export the query response to JSON.
   * @summary Export to JSON
   * @param file The file
   * @param formatter The cellset formatter
   * @param servletRequest The servlet request
   * @return A response containing a JSON query response.
   */
	@POST
	@Produces({"application/json" })
	@Path("/json/byactk")
	public Response exportJson(@FormParam("file") String file,
			@FormParam("formatter") String formatter,
			@FormParam("actk") String actk,
			@Context HttpServletRequest servletRequest)
	{
		String actkInfo;
		if (StringUtils.isBlank(actk)){
			return Response.serverError().entity("actk为空！无权访问！").status(Status.UNAUTHORIZED).build();
		}
		try {
			//这里只做actk的校验，如果解析成功则通过，不再获取用户信息
			actk = encryptAndDecryptUtil.decryptContentForRqpanda(actk);
		} catch (Throwable throwable){
			return Response.serverError().entity("actk解析失败！无权访问！").status(Status.UNAUTHORIZED).build();
		}

		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity(),"UTF-8");
			fileContent = ServletUtil.replaceParameters(servletRequest, fileContent);
			String queryName = UUID.randomUUID().toString();
//			query2Resource.createQuery(null,  null,  null, null, fileContent, queryName, null);
//			QueryResult qr = query2Resource.execute(queryName, formatter, 0);
			Map<String, String> parameters = ServletUtil.getParameters(servletRequest);
			ThinQuery tq = query2Resource.createQuery(queryName, fileContent, null, null);
			if (parameters != null) {
				tq.getParameters().putAll(parameters);
			}
		  if (StringUtils.isNotBlank(formatter)) {
			HashMap<String, Object> p = new HashMap<>();
			p.put("saiku.olap.result.formatter", formatter);
			if (tq.getProperties() == null) {
			  tq.setProperties(p);
			} else {
			  tq.getProperties().putAll(p);
			}
		  }
			QueryResult qr = query2Resource.execute(tq);
			return Response.ok().entity(qr).build();
		} catch (Exception e) {
			log.error("Error exporting JSON for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}

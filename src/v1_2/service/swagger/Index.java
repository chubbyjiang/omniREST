package v1_2.service.swagger;

import v1_2.service.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.File;

/**
 * Created by jchubby on 15/8/17.
 */

@Path("/doc/v1.2/{projectName}")
public class Index {
    @PathParam("projectName")
    String projectName;

    @GET
    @Path("/index.html")
    //@Produces(MediaType.TEXT_PLAIN)
    public String index() {
        String path = "/data2/usr/ibignose/tomcat/webapps/ROOT/doc/v1.2/" + projectName;
        return Util.readToString(new File(path + "/index.html"));
    }
}

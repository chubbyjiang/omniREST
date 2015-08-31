package v1_1.service.swagger;

import v1_1.service.Util;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import java.io.File;

/**
 * Created by jchubby on 15/8/17.
 */

@Path("/doc/v1.1/{projectName}")
public class Index {
    @PathParam("projectName")
    String projectName;

    @GET
    @Path("/index.html")
    //@Produces(MediaType.TEXT_PLAIN)
    public String index() {
        //String path = "/Users/jchubby/Documents/IdeaProjects/omniREST/out/artifacts/omniREST_war_exploded/docs/v1.1/" + projectName;
        String path = "/usr/ibignose/tomcat/webapps/ROOT/doc/v1.1/" + projectName;
        return Util.readToString(new File(path + "/index.html"));
    }
}

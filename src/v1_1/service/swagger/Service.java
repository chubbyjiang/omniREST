package v1_1.service.swagger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by jchubby on 15/8/17.
 */

@Path("/docService/v1.1/{projectName}")
public class Service {
    @PathParam("projectName")
    String projectName;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String docs() {
        String filePath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "v1_1/" + projectName + "/";
        File file = new File(filePath + "docs");
        return readToString(file);
    }

    @GET
    @Path("/{tablename}")
    @Produces(MediaType.TEXT_PLAIN)
    public String docs(@PathParam("tablename") String tablename) {
        String filePath = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "v1_1/" + projectName + "/";
        File file = new File(filePath + tablename);
        return readToString(file);
    }

    public String readToString(File file) {
        Long filelength = file.length();
        byte[] filecontent = new byte[filelength.intValue()];
        try {
            FileInputStream in = new FileInputStream(file);
            in.read(filecontent);
            in.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(filecontent, Charset.forName("utf-8"));
    }
}

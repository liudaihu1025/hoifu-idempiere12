package com.trekglobal.idempiere.rest.api.v1.auth.filter;  
  
import javax.annotation.Priority;  
import javax.ws.rs.Priorities;  
import javax.ws.rs.container.ContainerRequestContext;  
import javax.ws.rs.container.ContainerRequestFilter;  
import javax.ws.rs.container.ContainerResponseContext;  
import javax.ws.rs.container.ContainerResponseFilter;  
import javax.ws.rs.core.Response;  
import javax.ws.rs.ext.Provider;  
  
@Provider  
@Priority(Priorities.AUTHENTICATION - 100)  
public class CORSFilter implements ContainerRequestFilter, ContainerResponseFilter {  
      
    @Override  
    public void filter(ContainerRequestContext request) {  
        if ("OPTIONS".equals(request.getMethod())) {  
            request.abortWith(Response.ok().build());  
            return;  
        }  
    }  
      
    @Override  
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {  
        response.getHeaders().add("Access-Control-Allow-Origin", "*");  
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");  
        response.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");  
        response.getHeaders().add("Access-Control-Max-Age", "86400");  
    }  
}
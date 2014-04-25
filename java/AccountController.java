package com.dl.wbase.rest.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.dl.wbase.domain.Privilege;
import com.dl.wbase.domain.Role;
import com.dl.wbase.domain.UIDUser;
import com.dl.wbase.domain.User;
import com.dl.wbase.rest.util.Payload;
import com.dl.wbase.service.AccountService;
import com.dl.wbase.service.UIDUserService;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leandev.weaver.resource.MessageResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

import java.util.Map;

/**
 * Created by jasonlin on 1/27/14.
 */
@Controller
@RequestMapping("/rest/account")
public class AccountController {

    private static Logger logger = LoggerFactory.getLogger(AccountController.class);

    // Inject 訊息代碼的定義
    @Autowired
    private MessageResource messageResource;
    @Autowired
    private AccountService accountService;
    @Autowired
    private UIDUserService uidUserService;

    @RequestMapping(method = RequestMethod.GET, value = "/login")
    public ResponseEntity<Payload> login(@RequestParam(value="username") String username, @RequestParam(value="password") String password
            ,Locale locale,HttpSession session) {
        Payload payload = new Payload();
        User user = accountService.login(username, password);

        if(user.getId().equals("error")){
            payload.setCode("2003");
            payload.setStatus("N");
            payload.setMessage(messageResource.getMessage("2003", null, locale));
            return new ResponseEntity<Payload>(payload, HttpStatus.OK);
        }
        else if(user.getPassword().equals("error") )  {
            payload.setCode("2004");
            payload.setStatus("N");
            payload.setMessage(messageResource.getMessage("2004", null, locale));
            return new ResponseEntity<Payload>(payload, HttpStatus.OK);
            }
        else if(user.getId().equals(username)){
            session.setAttribute("id", user.getId());
            session.setAttribute("role", user.getRole());
            Map<String, Object> item;
            item = new HashMap();
            item.put("role", user.getRole());
            payload.addItem(item);
            payload.setStatus("Y");
            return new ResponseEntity<Payload>(payload, HttpStatus.OK);
        }
        else {
            return new ResponseEntity<Payload>(payload, HttpStatus.INTERNAL_SERVER_ERROR);       
        }
    }


    @RequestMapping(method = RequestMethod.GET, value = "/getPrivileges")
    public ResponseEntity<List<Privilege>> getPrivileges(HttpSession session) {
        String role = (String) session.getAttribute("role");
        List<Privilege> privileges = accountService.getPrivileges(role);                    
        return new ResponseEntity<List<Privilege>>(privileges, HttpStatus.OK);                     
 
    }
    
    @RequestMapping(method = RequestMethod.GET, value = "/getUserUID")
    public ResponseEntity<List<String>> getUserUID(HttpSession session) {
        String username = (String) session.getAttribute("id");
        
        List<String> finalList = this.uidUserService.queryUserUID(username);
                
        if(finalList!=null) {       
            return new ResponseEntity<List<String>>(finalList, HttpStatus.OK);
        } else {
            return new ResponseEntity<List<String>>(finalList, HttpStatus.FORBIDDEN);
        }
    }
    /*******************************for jsonp test***********************************/
    @RequestMapping(method = RequestMethod.GET, value = "/loginJsonp")
    public ResponseEntity<String> loginJsonp(
            @RequestParam(value="username") String username, 
            @RequestParam(value="password") String password,
            @RequestParam(value = "callback", required = false) String callback
            ,Locale locale,HttpSession session) {
    
        Payload payload = new Payload();    
        User user = accountService.login(username, password);
        String over = "wrong";

        if(user.getId().equals("error")){
            payload.setCode("2003");
            payload.setStatus("N");
            payload.setMessage(messageResource.getMessage("2003", null, locale));
            return new ResponseEntity<String>(over, HttpStatus.OK);
        }
        else if(user.getPassword().equals("error") )  {
            payload.setCode("2004");
            payload.setStatus("N");
            payload.setMessage(messageResource.getMessage("2004", null, locale));
            return new ResponseEntity<String>(over, HttpStatus.OK);
            }
        else if(user.getId().equals(username)){
            session.setAttribute("id", user.getId());
            session.setAttribute("role", user.getRole());
            Map<String, Object> item;
            item = new HashMap();
            item.put("role", user.getRole());
            payload.addItem(item);
            payload.setStatus("Y");
            
            if (callback != null && callback.trim().length() > 0) {
            
                    ObjectMapper mapper = new ObjectMapper();
                    String result ;
                    try {
                        System.out.println("writeValueAsString : ");
                        System.out.println(mapper.writeValueAsString(payload));
                        
                        System.out.println("result : ");
                        result  = mapper.writeValueAsString(payload);
                        
                        System.out.println(result);             
                        
                        result = callback + "(" + result + ")";
                        System.out.println("javascript result :");
                        System.out.println(result);
                        over = result;      
                    } catch (JsonGenerationException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (JsonMappingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }           
            }
            
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.setContentType(new MediaType( "application", "javascript" ));
             
            return new ResponseEntity<String>(over, responseHeaders, HttpStatus.OK);
            
        }
        else {
            return new ResponseEntity<String>(over, HttpStatus.INTERNAL_SERVER_ERROR);       
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
}

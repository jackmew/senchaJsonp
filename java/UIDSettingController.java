package com.dl.wbase.rest.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.dl.wbase.data.MongoStore;
import com.dl.wbase.data.WBStore;
import com.dl.wbase.domain.Privilege;
import com.dl.wbase.domain.UAID;
import com.dl.wbase.domain.UID;
import com.dl.wbase.domain.UIDDesc;
import com.dl.wbase.domain.UIDExam;
import com.dl.wbase.domain.UIDExamDetail;
import com.dl.wbase.domain.UIDUser;
import com.dl.wbase.domain.User;
import com.dl.wbase.rest.util.Payload;
import com.dl.wbase.service.AccountService;
import com.dl.wbase.service.UIDService;
import com.dl.wbase.service.UIDUserService;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leandev.weaver.resource.MessageResource;
import com.leandev.weaver.util.StringUtils;
import com.mongodb.WriteResult;

/**
 * Created by jackho on 2/6/14.
 */

@Controller
@RequestMapping("/rest/uid")
public class UIDSettingController {

	private static Logger logger = LoggerFactory.getLogger(UIDSettingController.class);

	@Autowired
    private UIDService uidService;
	
	@Autowired
	private UIDUserService uidUserService;
	// Inject 訊息代碼的定義
    @Autowired
    MessageResource messageResource;
		 
	 
	 /*modify 功能*/
	 @RequestMapping(method = RequestMethod.POST, value = "/modify")
	    public ResponseEntity<Payload> modify(@RequestBody List<Map> uidDescList, Locale locale,HttpSession session) {
		 	String username = (String) session.getAttribute("username");
		 	//其實只會取[0]
		 	String uid = (String) uidDescList.get(0).get("uid"); // uid 固定
		 	String description = (String) uidDescList.get(0).get("description"); 
		 	
		 	Payload payload = new Payload();
		 	
		 	logger.debug("saveModifyUIDDesc uid:"+uid);
		 	logger.debug("saveModifyUIDDesc description:"+description);
		 	
		 	UIDDesc uidDesc = new UIDDesc();
		 	Date date = new Date();
		 	
		 	uidDesc.setUid(uid);
		 	uidDesc.setDescription(description);
		 	uidDesc.setDeleteMark("N");	
		 	uidDesc.setCreatedTime(date);
		 	
		 	this.uidService.saveUIDDesc(uidDesc);
		 		
		 	updateUID_UIDDesc_createTime(uid,description,date,"N");
		 	
		 	payload.setStatus(HttpStatus.OK);
		 	payload.setCode("0002");
		 	payload.setMessage(messageResource.getMessage("0002", null, locale));
		 	
	        return new ResponseEntity (payload, HttpStatus.OK);
	       
	    }
	 /*新增功能 add*/
	 @RequestMapping(method = RequestMethod.POST, value = "/add")
	 	public ResponseEntity<Payload> add(@RequestBody List<Map> uidDescription,Locale locale, HttpSession session) {
		 	String username = (String) session.getAttribute("id");
		 	String role = (String) session.getAttribute("role");
		 	
		 	String condition = (String) uidDescription.get(0).get("uid");
		 	String description = (String) uidDescription.get(0).get("description");
		 	Payload payLoad = new Payload();
		 	
		 	//權限不等於S 才要審核
		 	if(!role.equals("S")){
			 	List<String> UIDUser = uidUserService.queryUserUID(username);
			 	boolean canNew = false;
			 	for(Iterator<String> iter = UIDUser.iterator(); iter.hasNext();){
			 		if(StringUtils.isStringLikeLength_3(iter.next(),condition)){
			 			canNew = true;
			 			break;
			 		}
			 	}
			 	if(!canNew){
			 		//非使用者有權限新增的UID
			 		payLoad.setCode("2001");
			 		payLoad.setMessage(messageResource.getMessage("2001", null, locale));
	    			payLoad.setStatus(HttpStatus.FORBIDDEN);
	    			return new ResponseEntity<Payload>(payLoad, HttpStatus.FORBIDDEN);
			 	}		 	
		 	}//結束審核
		 	
		 	List<UID> newuidList = uidService.findByUID(condition, UID.class, "UID");
		 	//不存在此uid 就新增一個
		 	if(newuidList.size()==0){
		 		int uaid_channel = condition.length();
		 		String UAID_X = "UAID_"+uaid_channel+"";
		 		Date d = new Date();
		 		//save覆蓋UID 加上description,deleteMark="N"
		 		UID newUID = new UID();
		 		
		 		newUID.setId(condition);
		 		newUID.setUaid(UAID_X);
		 		newUID.setUid(condition);
		 		newUID.setUidDesc_createdTime(d);
		 		newUID.setDeleteMark("N");
		 		newUID.setDescription(description);		 		
		 		//只在審查時 紀錄UAID  newUID.setUidField_createdTime(d);
		 		//新增時不需要寫值 uidFieldCreateTime 一定是tol-003審查核准時更新		 		
		 		uidService.saveUID(newUID);
		 		
		 		
		 		UIDDesc newUIDDesc = new UIDDesc();
		 		
		 		newUIDDesc.setCreatedTime(d);
		 		newUIDDesc.setDeleteMark("N");
		 		newUIDDesc.setDescription(description);
		 		newUIDDesc.setUid(condition);
		 		
		 		uidService.saveUIDDesc(newUIDDesc);
		 		//儲存成功
		 		payLoad.setCode("0002");
		 		payLoad.setMessage(messageResource.getMessage("0002", null, locale));
    			payLoad.setStatus(HttpStatus.OK);
    			return new ResponseEntity<Payload>(payLoad, HttpStatus.OK);
		 		
		 	}else{//存在uid
		 		
		 		String newdeleteMark = newuidList.get(0).getDeleteMark();
		 		if(newdeleteMark.equals("N")){//deleteMark = N
		 			//新增資料已經存在
		 			payLoad.setCode("1005");
			 		payLoad.setMessage(messageResource.getMessage("1005", null, locale));
	    			payLoad.setStatus(HttpStatus.FORBIDDEN);
	    			return new ResponseEntity<Payload>(payLoad, HttpStatus.FORBIDDEN);

		 		}else{//deleteMark = Y
	 			
		 			UIDDesc newUIDDesc = new UIDDesc();
		 			Date date = new Date();
		 			
		 			newUIDDesc.setCreatedTime(date);
		 			newUIDDesc.setDeleteMark("N");
		 			newUIDDesc.setDescription(description);
		 			newUIDDesc.setUid(condition);
		 			//存一筆UIDDesc deleteMark = N
		 			uidService.saveUIDDesc(newUIDDesc);
		 			
		 			//update UID  UIDDesc_createTime
		 			updateUID_UIDDesc_createTime(condition,description,date,"N");
		 			
		 			//儲存成功
			 		payLoad.setCode("0002");
			 		payLoad.setMessage(messageResource.getMessage("0002", null, locale));
	    			payLoad.setStatus(HttpStatus.OK);
	    			return new ResponseEntity<Payload>(payLoad, HttpStatus.OK);
		 			
		 		}
		 	}

	 }  
	 /*回寫 UIDDesc_createdTime 用的*/
	 	public void updateUID_UIDDesc_createTime(String condition,String description,Date date,String deleteMark){
		 	UID updateUID = (UID) uidService.findById(condition,UID.class);

			//主要就是更改時間 uidDesc_createTime
			updateUID.setUidDesc_createdTime(date);
			updateUID.setDeleteMark(deleteMark);
			updateUID.setDescription(description);
			uidService.saveUID(updateUID);
	 }
	 
	 
	 /**delete*/
	 	@RequestMapping(method = RequestMethod.GET, value = "/delete")
	 	public ResponseEntity<Payload> delete(@RequestParam(value="condition") String condition,Locale locale,HttpSession session) {
	 		Payload payload = new Payload();
	 		
	 		//找出一筆condition=uid的UID
	 		List<UID> newuidList = uidService.findByUIDandDeleteMark(condition, UID.class, "UID");
		 	//只有一筆 get(0)
		 	String channelUAID = newuidList.get(0).getUaid();
		 	
		 	List<UAID> newuaidList = uidService.queryUaids(condition, channelUAID);
		 	//沒有找到UAID
		 	if(newuaidList.size()==0){
		 		//更新UID,UIDDesc deleteMark = Y
		 		long createdTime = newuidList.get(0).getUidDesc_createdTime().getTime();
		 		
		 		List<UIDDesc> newuidDescList = uidService.findByUIDAndCreatedTime(condition,createdTime,UIDDesc.class,"UIDDesc");

		 		Date date = new Date();
		 		UIDDesc uidDesc = new UIDDesc();
		 		try{
		 			//更新UIDDes,UID
		 			String uid = newuidDescList.get(0).getUid();
		 			String description = newuidDescList.get(0).getDescription();
		 			//deleteMark="Y"	 			
		 			uidDesc.setUid(uid);
		 			uidDesc.setCreatedTime(date);		 			
		 			uidDesc.setDescription(description);
		 			uidDesc.setDeleteMark("Y");
		 			uidService.saveUIDDesc(uidDesc);
		 			//deleteMark="Y"
		 			updateUID_UIDDesc_createTime(condition,description,date,"Y");
		 			//儲存成功 
		 			payload.setCode("0000");
		 			payload.setMessage(messageResource.getMessage("0000", null, locale));
		 			payload.setStatus(HttpStatus.OK);
	    	
		 		}catch(NullPointerException e){
		 			//系統發生錯誤，請聯絡系統管理員！
		 			logger.warn("Exception: ", e);
		 			
		 			payload.setCode("1000");
		 			payload.setMessage(messageResource.getMessage("1000", null, locale));
		 			payload.setStatus(HttpStatus.FORBIDDEN);
	    			return new ResponseEntity<Payload>(payload, HttpStatus.FORBIDDEN);
		 			
		 		}
		 	//有找到UAID
		 	}else{
		 		String username = (String) session.getAttribute("username");
		 		long uidField_createdTime;
		 		Date date = new Date();
		 		try{
			 		//取得UidField_createdTime 並將它轉成long來查詢
			 		uidField_createdTime = newuidList.get(0).getUidField_createdTime().getTime();
			 		
			 		UIDExam uidExam = new UIDExam();
			 				 		
			 		uidExam.setAssessor(username);
			 		uidExam.setExamStatus("D");
			 		uidExam.setExamTime(date);
			 		uidExam.setUid(condition);
			 		//只存一筆審查主檔UIDExam
			 		uidService.saveUIDExam(uidExam);
		 		}catch(NullPointerException e){
		 			//系統發生錯誤，請聯絡系統管理員！
		 			logger.warn("Exception: ", e);
		 			
		 			payload.setCode("1000");
		 			payload.setMessage(messageResource.getMessage("1000", null, locale));
		 			payload.setStatus(HttpStatus.FORBIDDEN);
	    			return new ResponseEntity<Payload>(payload, HttpStatus.FORBIDDEN);
		 		}
		 			
		 		/**可能找到多筆一樣時間的UAID*/
		 		List<UAID> newuaidXList = uidService.findByUIDAndCheckTime(condition, uidField_createdTime, UAID.class,channelUAID);
		 		try{
		 			//多筆明細檔 UIDExamDetail
		 			for(int i = 0 ;i<newuaidXList.size();i++){
				 		String sequence = newuaidXList.get(i).getSequence();
				 		String language = newuaidXList.get(i).getLanguage();
				 		String name = newuaidXList.get(i).getName();
				 		String type = newuaidXList.get(i).getType();
				 		String content = newuaidXList.get(i).getContent();
				 		String unit = newuaidXList.get(i).getUnit();
				 		String fileName = newuaidXList.get(i).getFileName();
				 		
				 		UIDExamDetail uidExamDetailB = new UIDExamDetail();
				 		UIDExamDetail uidExamDetailA = new UIDExamDetail();
				 		
				 		uidExamDetailB.setSequence(sequence);
				 		uidExamDetailB.setLanguage(language);
				 		uidExamDetailB.setName(name);
				 		uidExamDetailB.setType(type);
				 		uidExamDetailB.setContent(content);
				 		uidExamDetailB.setUnit(unit);
				 		uidExamDetailB.setFileName(fileName);
				 		uidExamDetailB.setModifyTime(date);
				 		uidExamDetailB.setUid(condition);
				 		uidExamDetailB.setFlag("B");
				 		
				 		uidService.saveUIDExamDetail(uidExamDetailB);
				 		
				 		uidExamDetailA.setSequence(sequence);
				 		uidExamDetailA.setLanguage(language);
				 		uidExamDetailA.setName(name);
				 		uidExamDetailA.setType(type);
				 		uidExamDetailA.setContent(content);
				 		uidExamDetailA.setUnit(unit);
				 		uidExamDetailA.setFileName(fileName);
				 		uidExamDetailA.setModifyTime(date);
				 		uidExamDetailA.setUid(condition);
				 		uidExamDetailA.setFlag("A");
				 		
				 		uidService.saveUIDExamDetail(uidExamDetailA);
				 		//儲存成功 
			 			payload.setCode("0000");
			 			payload.setMessage(messageResource.getMessage("0000", null, locale));
			 			payload.setStatus(HttpStatus.OK);
		 			}//End for
		 		}catch(NullPointerException e){
		 			//系統發生錯誤，請聯絡系統管理員！
		 			logger.warn("Exception 有問題產生:找不到要刪除的UAID: ", e);
		 			
		 			payload.setCode("1000");
		 			payload.setMessage(messageResource.getMessage("1000", null, locale));
		 			payload.setStatus(HttpStatus.FORBIDDEN);
	    			return new ResponseEntity<Payload>(payload, HttpStatus.FORBIDDEN);

		 		}
		 		
		 	}//end else	        
		 		return new ResponseEntity<Payload>(payload, HttpStatus.OK);
	 	}
	 
	 	/*要不要hide 新增 validateRole*/
		 @RequestMapping(method = RequestMethod.GET, value = "/validateRole")
		    public ResponseEntity<Map> validateRole(HttpSession session) {
			 	String role = (String) session.getAttribute("role");
			 	String userID = (String) session.getAttribute("id");
			 	
			 	User user = (User) uidService.findById(userID, User.class);
			 	String createUID = user.getCreateUid();
			 	
			 	Map m = new HashMap();
			 	
			 	m.put("role", role);
			 	m.put("createUID", createUID);

		        if(role!=null) {
		            return new ResponseEntity<Map>(m, HttpStatus.OK);
		        } else {
		            return new ResponseEntity<Map>(m, HttpStatus.FORBIDDEN);
		        }
		   }
		
		
		
		 
		 @RequestMapping(method = RequestMethod.GET, value = "/queryOnePage")
		    public ResponseEntity<List<UID>> queryOnePage(@RequestParam(value="currBlock") int currBlock,
		    		@RequestParam(value="condition") String condition,HttpSession session) {

			 String username = (String) session.getAttribute("id");
			 String role = (String) session.getAttribute("role");
			 int offset = (currBlock-1)*100;
			 long count = 0;
			 List<UID> finalList = new ArrayList();
			 			 
			 			
			 if(role.equals("S")){

		    		if(condition.equals("")){
		    		long startTime = new Date().getTime();
		    			finalList = uidService.findByUAID_3AndDeleteMark_NWithSkipAndLimit(offset);
		    		long endTime = new Date().getTime();
		    		long differTime = endTime - startTime;
		    		logger.debug("query time :"+differTime+"milliseconds");
		    			//count = uidService.countUAID_3AndDeleteMark_N();

					 }else{
						finalList = uidService.findByUIDAndDeleteMark_NWithSkipAndLimit(condition,offset);
						//count = uidService.countByRegexUIDAndDeleteMark_N(condition);
					}

		     }else{//role != S 就只能查user有權限查的UID
		    	 
		    	 	finalList = uidService.findByUserUIDAndDeleteMark_NWithSkipAndLimit(condition,username,offset);
		    	 	//count = uidService.countByUserUIDAndDeleteMark_NWithSkipAndLimit(condition,username);
		    }
			 			 	
		        if(finalList!=null) {
		            return new ResponseEntity<List<UID>>(finalList, HttpStatus.OK);
		        } else {
		            return new ResponseEntity<List<UID>>(finalList, HttpStatus.FORBIDDEN);
		        }
		    }		 		 
/**********************************************for jsonp test******************************************************/		 
		 @RequestMapping(method = RequestMethod.GET, value = "/jsonp")
		    public ResponseEntity<String> jsonp(@RequestParam(value = "callback", required = false) String callback
		    		) {
			 	//String result = "{'id':'ZAAAAA','uid':'ZAAAAA','uidDesc_createdTime':1393921484707,'uidField_createdTime':1393921484707,'uaid':'UAID_6','description':'uid DESC:53159aefc7d10bbb7d0d43e1','deleteMark':'N','priority':0,'version':0} ";
			 	 String result = "{\"Spring\":10000}";
			 	 if (callback != null && callback.trim().length() > 0) {
			 		   result = callback + "(" + result + ")";
			 		  }
			 	HttpHeaders responseHeaders = new HttpHeaders();
			 	responseHeaders.setContentType(new MediaType( "application", "javascript" ));
			 	 
			 	return new ResponseEntity<String>(result, responseHeaders, HttpStatus.OK);
		    }
		 
		 @RequestMapping(method = RequestMethod.GET, value = "/queryOnePageJsonp")
		    public ResponseEntity<String> queryOnePageJsonp(
		    		@RequestParam(value="currBlock") int currBlock,
		    		@RequestParam(value="condition") String condition,HttpSession session,
		    		@RequestParam(value = "callback", required = false) String callback
		    		) {

			 String username = (String) session.getAttribute("id");
			 String role = (String) session.getAttribute("role");
			 int offset = (currBlock-1)*100;
			 long count = 0;
			 List<UID> finalList = new ArrayList();
			 ObjectMapper mapper = new ObjectMapper();
			 String result ;
			 String resultRemoveBracket;
			 String javascriptResult ="";
			 			 
			 			
			 finalList = uidService.findByUIDAndDeleteMark_NWithSkipAndLimit(condition,offset);
			 
			try {
				result  = mapper.writeValueAsString(finalList);
				System.out.println("finalList to json string:");	
				System.out.println(result);	
				resultRemoveBracket = result.substring(1,result.length()-1);
				System.out.println("json string remove bracket:");
				System.out.println(resultRemoveBracket);				
				javascriptResult = callback + "(" + resultRemoveBracket + ")";
				System.out.println("javascript json string :");
				System.out.println(javascriptResult);
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
			 			 	
		    
			HttpHeaders responseHeaders = new HttpHeaders();
		 	responseHeaders.setContentType(new MediaType( "application", "javascript" ));
		 	 
		 	return new ResponseEntity<String>(javascriptResult, responseHeaders, HttpStatus.OK);
		        
		    }

		 
	 
} 
	 
package nara_bid_information;

public class EndPoints {
	public final static String NOTI_BASE_PATH = "http://apis.data.go.kr/1230000/BidPublicInfoService/";
	public final static String RES_BASE_PATH = "http://apis.data.go.kr/1230000/ScsbidInfoService/";
	
	public final static String PROD_NOTI = "getBidPblancListInfoThng?";
	public final static String FACIL_NOTI = "getBidPblancListInfoCnstwk?";
	public final static String SERV_NOTI = "getBidPblancListInfoServc?";
	public final static String WEJA_NOTI = "getBidPblancListInfoFrgcpt?";
	
	public final static String PROD_BASE_PRICE = "getBidPblancListInfoThngBsisAmount?";
	public final static String FACIL_BASE_PRICE = "getBidPblancListInfoCnstwkBsisAmount?";
	public final static String SERV_BASE_PRICE = "getBidPblancListInfoServcBsisAmount?";
	public final static String WEJA_BASE_PRICE = "getBidPblancListInfoFrgcptBsisAmount?";
	
	public final static String PROD_RENOTI = "getBidRepblancListInfoThng?";
	public final static String FACIL_RENOTI = "getBidRepblancListInfoFclty?";
	public final static String SERV_RENOTI = "getBidRepblancListInfoServc?";
	public final static String WEJA_RENOTI = "getBidRepblancListInfoFrgcpt?";
	
	public final static String PROD_REBASE_PRICE = "getBidRepblancListInfoThngBsisAmount?";
	public final static String FACIL_REBASE_PRICE = "getBidRepblancListInfoFcltyBsisAmount?";
	public final static String SERV_REBASE_PRICE = "getBidRepblancListInfoServcBsisAmount?";
	public final static String WEJA_REBASE_PRICE = "getBidRepblancListInfoFrgcptBsisAmount?";
	
	public final static String PROD_TOTAL_NOTI = "getThngBidPblancListSttusThng?";
	public final static String FACIL_TOTAL_NOTI = "getFcltyBidRepblancListMetaInfoFclty?";
	public final static String SERV_TOTAL_NOTI = "getServcBidPblancListMetaInfoServc?";
	
	public final static String PROD_TOTAL_BASE_PRICE = "getThngBidPblancListSttusThngBsisAmount?";
	public final static String FACIL_TOTAL_BASE_PRICE = "getFcltyBidRepblancListMetaInfoFcltyBsisAmount?";
	public final static String SERV_TOTAL_BASE_PRICE = "getServcBidPblancListMetaInfoServcBsisAmount?";
	
	public final static String PROD_RES = "getOpengResultListInfoThng?";
	public final static String FACIL_RES = "getOpengResultListInfoCnstwk?";
	public final static String SERV_RES = "getOpengResultListInfoServc?";
	public final static String WEJA_RES = "getOpengResultListInfoFrgcpt?";
	
	public final static String PROD_PRE_PRICE = "getOpengResultListInfoThngPreparPcDetail?";
	public final static String FACIL_PRE_PRICE = "getOpengResultListInfoCnstwkPreparPcDetail?";
	public final static String SERV_PRE_PRICE = "getOpengResultListInfoServcPreparPcDetail?";
	public final static String WEJA_PRE_PRICE = "getOpengResultListInfoFrgcptPreparPcDetail?";
	
	public final static String NOTI_LICENSE = "getBidPblancListInfoLicenseLimit?";
}

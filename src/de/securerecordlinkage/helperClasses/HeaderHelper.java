package de.securerecordlinkage.helperClasses;

import java.util.ArrayList;

public class HeaderHelper {

    public static ArrayList<Header> addHeaderToExistingHeaderArrayList(ArrayList<Header> headerArrayList, String headerKey, String headerValue){
        Header httpHeader = new Header(headerKey, headerValue);
        headerArrayList.add(httpHeader);
        return headerArrayList;
    }

    public static ArrayList<Header> addHeaderToNewCreatedArrayList(String headerKey, String headerValue){
        ArrayList<Header> headerArrayList = new ArrayList<>();
        Header httpHeader = new Header(headerKey, headerValue);
        headerArrayList.add(httpHeader);
        return headerArrayList;
    }
}

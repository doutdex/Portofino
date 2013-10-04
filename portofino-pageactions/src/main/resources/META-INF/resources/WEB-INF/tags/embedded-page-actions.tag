<%@ tag import="org.slf4j.LoggerFactory"
%><%@ attribute name="list" required="true"
%><%@ attribute name="cssClass" required="false"
%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<jsp:useBean id="actionBean" scope="request"
             type="com.manydesigns.portofino.pageactions.AbstractPageAction"/>
<div class="${cssClass} embeddedPageAction">
    <input type="hidden" name="embeddedPageActionWrapperName_${list}" value="embeddedPageActionWrapper_${list}" />
    <% actionBean.initEmbeddedPageActions(); %>
    <c:forEach var="embeddedPageAction" items="${ actionBean.embeddedPageActions[list] }">
        <div id="embeddedPageActionWrapper_<c:out value='${embeddedPageAction.id}' />">
            <% try {%>
                <jsp:include page="${embeddedPageAction.path}" flush="false" />
            <%} catch (Throwable t) {
                LoggerFactory.getLogger(actionBean.getClass()).error("Error in included page", t);
            %>
                <div class="alert alert-error">
                    <button data-dismiss="alert" class="close" type="button">&times;</button>
                    <ul class="errorMessages">
                        <li>
                            <fmt:message key="pageaction.view.error">
                                <fmt:param value="${embeddedPageAction.path}" />
                            </fmt:message>
                        </li>
                    </ul>
                </div>
            <%}%>
        </div>
    </c:forEach>
</div>
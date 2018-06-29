package jstech.edu.transportmodel.common;

public enum RouteStatus {
    YET_TO_START, IN_TRANSIT, COMPLETED;

    public static RouteStatus getRouteStatus(String status) {
        status = status.replaceAll("[^a-zA-Z]", "");
        for(RouteStatus routeStatus: values()) {
            String tmpStatus = routeStatus.toString().replaceAll("[^a-zA-Z]", "");
            if(tmpStatus.equalsIgnoreCase(status)) {
                return routeStatus;
            }
        }
        return null;
    }
}

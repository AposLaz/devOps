package lgl.bayern.de.ecertby.repository;

import lgl.bayern.de.ecertby.model.Notification;
import lgl.bayern.de.ecertby.model.QNotification;
import lgl.bayern.de.ecertby.model.util.ViewThreadVisibility;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.binding.QuerydslBindings;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends BaseRepository<Notification, QNotification> {
    @Query("SELECT n " +
            "FROM Notification n " +
            "WHERE n.status = 'PUBLISHED_NOTIFICATION' AND n.active=true " +
            "AND n.validFrom <= CURRENT_TIMESTAMP " +
            "AND (n.validTo IS NULL OR n.validTo >= CURRENT_TIMESTAMP) " +
            "AND (n.userViewOptions = 'VISIBLE_TO_ALL' OR n.userViewOptions = :userVisibility) " +
            "AND (n.pageViewOptions = 'VISIBLE_TO_HOME_PAGE' OR n.pageViewOptions = 'VISIBLE_TO_ALL_PAGES')")
    Notification findCurrentPublishedForHomePage(ViewThreadVisibility userVisibility);

    @Query("SELECT n " +
            "FROM Notification n " +
            "WHERE n.status = 'PUBLISHED_NOTIFICATION' AND n.active=true " +
            "AND n.validFrom <= CURRENT_TIMESTAMP " +
            "AND (n.validTo IS NULL OR n.validTo >= CURRENT_TIMESTAMP) " +
            "AND (n.userViewOptions = 'VISIBLE_TO_ALL' OR n.userViewOptions = :userVisibility) " +
            "AND (n.pageViewOptions = 'VISIBLE_TO_ALL_PAGES')")
    Notification findCurrentPublishedForAllPages(ViewThreadVisibility userVisibility);

    Notification findByTitle(String title);

    Notification findByTitleAndIdNot(String title, String id);

    @Query("SELECT n FROM Notification n " +
            "WHERE ((n.validFrom <= :validTo AND n.validTo >= :validFrom) " +
            "OR (n.validFrom >= :validFrom AND n.validTo <= :validTo) " +
            "OR (n.validFrom <= :validFrom AND n.validTo >= :validTo)) " +
            "AND (COALESCE(:visibility, 'VISIBLE_TO_ALL') = 'VISIBLE_TO_ALL' " +
            "OR (n.userViewOptions = 'VISIBLE_TO_ALL' " +
            "AND n.userViewOptions IN (lgl.bayern.de.ecertby.model.util.ViewThreadVisibility.VISIBLE_TO_AUTHORITIES, lgl.bayern.de.ecertby.model.util.ViewThreadVisibility.VISIBLE_TO_COMPANIES,lgl.bayern.de.ecertby.model.util.ViewThreadVisibility.VISIBLE_TO_ALL)) " +
            "OR n.userViewOptions = COALESCE(:visibility, n.userViewOptions))")
    List<Notification> findOverlappingNotifications(
            @Param("validFrom") Instant validFrom,
            @Param("validTo") Instant validTo,
            @Param("visibility") ViewThreadVisibility visibility);


    @Override
    default void customize(QuerydslBindings bindings, QNotification notification) {
        // Call the common customization first
        BaseRepository.super.customize(bindings, notification);
        bindings.bind(notification.validFrom).first((path, value) ->
                notification.validFrom.goe(value));
        bindings.bind(notification.validTo).first((path, value) ->
                notification.validTo.loe(value).or(notification.validTo.isNull()));
    }
}

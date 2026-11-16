package com.rahulpranaya.iggate;

/**
 * Screen rules. The service fills a {@link View} from Instagram's controls,
 * then this decides whether to stay or open the inbox.
 */
final class Policy {
    enum Place {
        NONE,
        HOME,
        CHAT
    }

    enum Action {
        STAY,
        GO_TO_INBOX
    }

    static final class View {
        boolean reelsTabSelected;
        boolean searchTabSelected;
        boolean searchPageOpen;
        boolean fullScreenReel;
        boolean messagesOpen;
        boolean homeSelected;
    }

    static final class Result {
        final Action action;
        final Place place;

        Result(Action action, Place place) {
            this.action = action;
            this.place = place;
        }
    }

    private Policy() {
    }

    static Result decide(View view, Place place) {
        boolean blockedTab = view.searchTabSelected || view.searchPageOpen;
        Place next = place;
        if (!view.fullScreenReel && !view.reelsTabSelected && !blockedTab) {
            if (view.messagesOpen) {
                next = Place.CHAT;
            } else if (view.homeSelected) {
                next = Place.HOME;
            }
        }
        Action action = blockedTab ? Action.GO_TO_INBOX : Action.STAY;
        return new Result(action, next);
    }
}

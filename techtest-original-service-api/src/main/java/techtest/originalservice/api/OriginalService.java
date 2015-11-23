package techtest.originalservice.api;

/**
 * A more formal typed interface for the original service.
 * 
 * This might be overkill for the requirements of techtest, but might be appropriate if the service
 * is used in multiple places.
 */
public interface OriginalService {
    public Bet[] available();
    public BetsResponse bets(BetsRequest bet);

    public static enum Error {
        UNKNOWN(0, "Unknown"), INCORRECT_ODDS(1, "Incorrect Odds"), INVALID_ODDS(2, "Invalid Odds"), INVALID_BET_ID(3,
                "Invalid Bet ID"), INVALID_STAKE(4, "Invalid Stake");

        private final int code;
        private final String description;

        private Error(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return code + ": " + description;
        }
    }

    /**
     * An exception representing a 'business logic' like problem .
     */
    public static class BusinessLogicException extends RuntimeException {
        private Error error;

        public BusinessLogicException(Error error) {
            this(error, null);
        }

        public BusinessLogicException(Error error, Throwable cause) {
            super(error.description, cause);
            this.error = error;
        }

        public Error getError() {
            return error;
        }
    }

    /**
     * An exception representing something non-business has gone wrong.
     */
    public static class InternalErrorException extends RuntimeException {
        private String message;

        public InternalErrorException(String message) {
            super(message);
        }

        public InternalErrorException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

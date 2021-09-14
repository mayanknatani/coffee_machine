package server.exceptions;

import lombok.Getter;

@Getter
public class IngredientNotAvailableException extends RuntimeException {
    private int required;
    private int available;
    private String ingredientName;

    public IngredientNotAvailableException(String message, String ingredientName, int required, int available) {
        super(message);
        this.ingredientName = ingredientName;
        this.required = required;
        this.available = available;
    }

    @Override
    public String getMessage() {
        return super.getMessage() + " " + this.ingredientName + ", required " + required + ", available " + available;
    }
}

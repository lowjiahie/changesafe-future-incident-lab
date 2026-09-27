package com.ttulka.ecommerce.warehouse;

/**
 * Warehouse use-cases.
 */
public interface Warehouse {

    /**
     * Returns stock details for a product.
     *
     * @param productId the ID of the product
     * @return the stock details
     */
    InStock leftInStock(ProductId productId);

    /**
     * Returns stock details for a product under a pessimistic read lock (SELECT ... FOR UPDATE).
     * Must be called within an active transaction. Prevents concurrent checkouts from both
     * passing the stock check for the last unit (BR-013).
     *
     * <p>The default implementation falls back to {@link #leftInStock(ProductId)} for
     * non-JDBC implementations (e.g. mocks, in-memory stubs) that do not support locking.
     *
     * @param productId the ID of the product
     * @return the stock details
     */
    default InStock leftInStockForUpdate(ProductId productId) {
        return leftInStock(productId);
    }

    /**
     * Puts product items into the stock.
     *
     * @param productId the ID of the product
     * @param amount    the amount of items
     */
    void putIntoStock(ProductId productId, Amount amount);
}

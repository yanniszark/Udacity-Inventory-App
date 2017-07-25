package com.example.android.inventoryapp;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.ProductEntry;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * {@link ProductCursorAdapter} is an adapter for a list or grid view
 * that uses a {@link Cursor} of product data as its data source.
 */
public class ProductCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link ProductCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public ProductCursorAdapter(Context context, Cursor c) {
        super(context, c, 0 /* flags */);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate a list item view using the layout specified in list_item.xml
        View view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false);
        view.setTag(new ViewHolder(view));
        return view;
    }

    /**
     * This method binds the product data (in the current row pointed to by cursor) to the given
     * list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    @Override
    public void bindView(View view, final Context context, final Cursor cursor) {
        // Retrieve the ViewHolder
        ViewHolder holder = (ViewHolder) view.getTag();

        // Find the columns of product attributes that we're interested in
        int idColumnIndex = cursor.getColumnIndex(ProductEntry._ID);
        int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
        int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
        int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
        int pictureColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PICTURE);

        // Read the product attributes from the Cursor for the current product
        final int productId = cursor.getInt(idColumnIndex);
        String productName = cursor.getString(nameColumnIndex);
        int productPrice = cursor.getInt(priceColumnIndex);
        final int productQuantity = cursor.getInt(quantityColumnIndex);
        //TODO: Retrieve picture


        // Update the TextViews with the attributes for the current pet
        holder.productNameTextView.setText(productName);
        holder.productPriceTextView.setText(ProductEntry.PRODUCT_PRICE_CURRENCY + " " + String.valueOf(productPrice));
        holder.productQuantityTextView.setText(String.valueOf(productQuantity));
        //TODO: Setup OnClickListener for the SALE button
        holder.productSaleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int rowsAffected = 0;
                switch (productQuantity) {
                    case ProductEntry.MINIMUM_QUANTITY:
                        rowsAffected = 0;
                        break;
                    default:
                        /* Create the values to update */
                        ContentValues values = new ContentValues();
                        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, productQuantity - 1);
                        /* Create the correct URI to update the specific product */
                        Uri currentProductUri = ContentUris.withAppendedId(ProductEntry.CONTENT_URI, productId);
                        /* Execute the update */
                        rowsAffected = context.getContentResolver().update(currentProductUri, values, null, null);
                }

                // Show a toast message depending on whether or not the update was successful.
                if (rowsAffected == 0) {
                    // If no rows were affected, then there was an error with the update.
                    Toast.makeText(context, context.getString(R.string.editor_activity_update_product_failed),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Otherwise, the update was successful and we can display a toast.
                    Toast.makeText(context, context.getString(R.string.editor_activity_update_product_successfull),
                            Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    static class ViewHolder {
        @BindView(R.id.product_name)
        TextView productNameTextView;
        @BindView(R.id.product_price)
        TextView productPriceTextView;
        @BindView(R.id.product_quantity)
        TextView productQuantityTextView;
        @BindView(R.id.product_picture)
        ImageView productPictureImageView;
        @BindView(R.id.product_button_sale)
        Button productSaleButton;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}

package com.example.android.inventoryapp;

import android.app.AlertDialog;
import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.android.inventoryapp.data.InventoryContract.ProductEntry;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.example.android.inventoryapp.data.InventoryContract.ProductEntry.MINIMUM_QUANTITY;

/**
 * Allows user to create a new product or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {


    private static final String LOG_TAG = EditorActivity.class.getSimpleName();

    /* Identifier for load image result */
    private static final int RESULT_LOAD_IMAGE = 145;

    /**
     * Identifier for the product data loader
     */
    private static final int EXISTING_PRODUCT_LOADER = 0;

    /**
     * Content URI for the existing product (null if it's a new product)
     */
    private Uri mCurrentProductUri;

    /**
     * EditText field to enter the product's name
     */
    @BindView(R.id.edit_product_name)
    EditText productNameEditText;

    /**
     * EditText field to enter the product's price
     */
    @BindView(R.id.edit_product_price)
    EditText productPriceEditText;

    /**
     * TextView field to display the product's quantity and Button(s) to modify it
     */
    @BindView(R.id.textview_product_quantity)
    TextView productQuantityTextView;
    @BindView(R.id.button_quantity_decrement)
    Button quantityDecrementButton;
    @BindView(R.id.button_quantity_increment)
    Button quantityIncrementButton;

    /**
     * ImageView field for the product's image
     */
    @BindView(R.id.edit_product_picture)
    ImageView productPictureImageView;

    /**
     * EditText field to enter the supplier's name
     */
    @BindView(R.id.edit_product_supplier)
    EditText productSupplierEditText;

    /**
     * EditText field to enter the supplier's mail
     */
    @BindView(R.id.edit_product_supplier_mail)
    EditText productSupplierMailEditText;

    /**
     * Temporarily store the supplier mail
     */
    String mSupplierMail;

    /**
     * Boolean flag that keeps track of whether the product has been edited (true) or not (false)
     */
    private boolean productHasChanged = false;


    /**
     * OnTouchListener that listens for any user touches on a View, implying that they are modifying
     * the view, and we change the productHasChanged boolean to true.
     */
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            productHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        // Examine the intent that was used to launch this activity,
        // in order to figure out if we're creating a new product or editing an existing one.
        Intent intent = getIntent();
        mCurrentProductUri = intent.getData();

        // If the intent DOES NOT contain a product content URI, then we know that we are
        // creating a new product.
        if (mCurrentProductUri == null) {
            // This is a new product, so change the app bar to say "Add a product"
            setTitle(getString(R.string.editor_activity_title_new_product));

            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit product"
            setTitle(getString(R.string.editor_activity_title_edit_product));

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PRODUCT_LOADER, null, this);
        }

        // Find all relevant views that we will need to read user input from
        ButterKnife.bind(this);


        // Setup OnTouchListeners on all the input fields, so we can determine if the user
        // has touched or modified them. This will let us know if there are unsaved changes
        // or not, if the user tries to leave the editor without saving.
        productNameEditText.setOnTouchListener(mTouchListener);
        productPriceEditText.setOnTouchListener(mTouchListener);
        productSupplierEditText.setOnTouchListener(mTouchListener);
        productSupplierMailEditText.setOnTouchListener(mTouchListener);

        quantityDecrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantity = Integer.valueOf(productQuantityTextView.getText().toString());
                if (quantity > ProductEntry.MINIMUM_QUANTITY)
                    productQuantityTextView.setText(String.valueOf(quantity - 1));

            }
        });
        quantityIncrementButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int quantity = Integer.valueOf(productQuantityTextView.getText().toString());
                productQuantityTextView.setText(String.valueOf(quantity + 1));
            }
        });

        productPictureImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent;

                if (Build.VERSION.SDK_INT < 19) {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                } else {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                }

                intent.setType("image/*");
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), RESULT_LOAD_IMAGE);
            }
        });

    }


    /**
     * Get user input from editor and save product into database.
     */
    private boolean saveProduct() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = productNameEditText.getText().toString().trim();
        if (nameString.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_enter_product_name), Toast.LENGTH_SHORT).show();
            return false;
        }
        String priceString = productPriceEditText.getText().toString().trim();
        if (priceString.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_enter_product_price), Toast.LENGTH_SHORT).show();
            return false;
        }

        String quantityString = productQuantityTextView.getText().toString().trim();
        String supplierString = productSupplierEditText.getText().toString().trim();
        if (supplierString.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_enter_product_supplier), Toast.LENGTH_SHORT).show();
            return false;
        }
        String supplierMailString = productSupplierMailEditText.getText().toString().trim();
        if (supplierMailString.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_enter_product_supplier_mail), Toast.LENGTH_SHORT).show();
            return false;
        }

        // Check if this is supposed to be a new product
        // and check if all the fields in the editor are blank
        if (mCurrentProductUri == null &&
                TextUtils.isEmpty(nameString) && TextUtils.isEmpty(priceString) &&
                TextUtils.isEmpty(quantityString) && TextUtils.isEmpty(supplierString) &&
                TextUtils.isEmpty(supplierMailString)) {
            // Since no fields were modified, we can return early without creating a new product.
            // No need to create ContentValues and no need to do any ContentProvider operations.
            Toast.makeText(this, "No new product saved", Toast.LENGTH_SHORT).show();
            return true;
        }

        // Create a ContentValues object where column names are the keys,
        // and product attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(ProductEntry.COLUMN_PRODUCT_NAME, nameString);
        values.put(ProductEntry.COLUMN_PRODUCT_PRICE, priceString);
        values.put(ProductEntry.COLUMN_PRODUCT_QUANTITY, quantityString);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER, supplierString);
        values.put(ProductEntry.COLUMN_PRODUCT_SUPPLIER_MAIL, supplierMailString);
        //Check if there is a new image
        Uri uri = (Uri) productPictureImageView.getTag(R.string.TAG_KEY_IMAGE_URI);
        if (uri != null)
            values.put(ProductEntry.COLUMN_PRODUCT_PICTURE, uri.toString());

        // Determine if this is a new or existing product by checking if mCurrentproductUri is null or not
        if (mCurrentProductUri == null) {
            // This is a NEW product, so insert a new product into the provider,
            // returning the content URI for the new product.
            Uri newUri = getContentResolver().insert(ProductEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_activity_insert_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_activity_insert_product_successfull),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            // Otherwise this is an EXISTING product, so update the product with content URI: mCurrentProductUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentProductUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update(mCurrentProductUri, values, null, null);

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText(this, getString(R.string.editor_activity_update_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_activity_update_product_successfull),
                        Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new product, hide the "Delete" menu item.
        if (mCurrentProductUri == null) {
            MenuItem menuItem = menu.findItem(R.id.action_delete);
            menuItem.setVisible(false);
            menuItem = menu.findItem(R.id.action_reorder);
            menuItem.setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save product to database
                if (saveProduct())
                    finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Reorder" menu option
            case R.id.action_reorder:
                Intent intent = new Intent(Intent.ACTION_SENDTO);
                intent.setData(Uri.parse("mailto:" + mSupplierMail)); // only email apps should handle this
                startActivity(intent);
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the product hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!productHasChanged) {
                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;
                }

                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog(discardButtonClickListener);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the product hasn't changed, continue with handling back button press
        if (!productHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };

        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all product attributes, define a projection that contains
        // all columns from the product table
        String[] projection = {
                ProductEntry._ID,
                ProductEntry.COLUMN_PRODUCT_NAME,
                ProductEntry.COLUMN_PRODUCT_PRICE,
                ProductEntry.COLUMN_PRODUCT_QUANTITY,
                ProductEntry.COLUMN_PRODUCT_PICTURE,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER,
                ProductEntry.COLUMN_PRODUCT_SUPPLIER_MAIL};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader(this,   // Parent activity context
                mCurrentProductUri,         // Query the content URI for the current product
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Bail early if the cursor is null or there is less than 1 row in the cursor
        if (cursor == null || cursor.getCount() < 1) {
            return;
        }

        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of product attributes that we're interested in
            int nameColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_NAME);
            int priceColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PRICE);
            int quantityColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_QUANTITY);
            int pictureColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_PICTURE);
            int supplierColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER);
            int supplierMailColumnIndex = cursor.getColumnIndex(ProductEntry.COLUMN_PRODUCT_SUPPLIER_MAIL);

            // Read the product attributes from the Cursor for the current product
            String productName = cursor.getString(nameColumnIndex);
            int productPrice = cursor.getInt(priceColumnIndex);
            int productQuantity = cursor.getInt(quantityColumnIndex);
            String productSupplier = cursor.getString(supplierColumnIndex);
            String productSupplierMail = cursor.getString(supplierMailColumnIndex);
            String productPicture = cursor.getString(pictureColumnIndex);
            mSupplierMail = productSupplierMail;

            // Update the views on the screen with the values from the database
            productNameEditText.setText(productName);
            productPriceEditText.setText(String.valueOf(productPrice));
            productQuantityTextView.setText(String.valueOf(productQuantity));
            productSupplierEditText.setText(productSupplier);
            productSupplierMailEditText.setText(productSupplierMail);
            if (productPicture != null && !productPicture.isEmpty())
                Glide
                        .with(this)
                        .load(Uri.parse(productPicture))
                        .into(productPictureImageView);

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        productNameEditText.setText("");
        productPriceEditText.setText("");
        productQuantityTextView.setText(String.valueOf(MINIMUM_QUANTITY));
        productSupplierEditText.setText("");
        productSupplierMailEditText.setText("");
    }

    /**
     * Show a dialog that warns the user there are unsaved changes that will be lost
     * if they continue leaving the editor.
     *
     * @param discardButtonClickListener is the click listener for what to do when
     *                                   the user confirms they want to discard their changes
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);
        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this product.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the product.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the product.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the product in the database.
     */
    private void deleteProduct() {
        // Only perform the delete if this is an existing product.
        if (mCurrentProductUri != null) {
            // Call the ContentResolver to delete the product at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentproductUri
            // content URI already identifies the product that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentProductUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_activity_delete_product_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_activity_delete_product_successfull),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri mUri = data.getData();
            Log.i(LOG_TAG, "Uri: " + mUri.toString());

            /* Set the product image to the new image */
            Glide
                    .with(this)
                    .load(mUri)
                    .into(productPictureImageView);

            /* Store image uri as ImageView tag */
            productPictureImageView.setTag(R.string.TAG_KEY_IMAGE_URI, mUri);
            /* Set the changed flag to true */
            productHasChanged = true;

        }


    }
}
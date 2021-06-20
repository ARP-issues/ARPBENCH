package openfoodfacts.github.scrachx.openfood.views.category.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.List;

import openfoodfacts.github.scrachx.openfood.category.model.Category;
import openfoodfacts.github.scrachx.openfood.databinding.CategoryRecyclerItemBinding;

/**
 * Created by Abdelali Eramli on 27/12/2017.
 */

public class CategoryListRecyclerAdapter extends RecyclerView.Adapter<CategoryViewHolder> {
    private final List<Category> categories;

    public CategoryListRecyclerAdapter(List<Category> categories) {
        this.categories = categories;
    }

    @Override
    public CategoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        CategoryRecyclerItemBinding binding = CategoryRecyclerItemBinding.inflate(LayoutInflater.from(parent.getContext()),
                parent, false);
        return new CategoryViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(CategoryViewHolder holder, int position) {
        holder.bind(categories.get(position));
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }
}

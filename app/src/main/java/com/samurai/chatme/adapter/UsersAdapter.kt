package com.samurai.chatme.adapter


import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.samurai.chatme.User

import com.samurai.chatme.databinding.ItemUsersBinding


class UsersAdapter(
    private var users: List<User>,
    private val onItemClick: (User) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(private val binding: ItemUsersBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {

            binding.tvUserName.text = user.name

            Log.d("UsersAdapter", "User: ${user.name}, UnreadCount: ${user.unreadCount}")
            binding.tvUserName.text = user.name
            binding.tvUnreadCount.text = "" // Eski matnni tozalash
            if (user.unreadCount > 0) {
                binding.tvUnreadCount.text = user.unreadCount.toString()
                binding.tvUnreadCount.visibility = View.VISIBLE
                Log.d("UsersAdapter", "tvUnreadCount VISIBLE for ${user.name}")
            } else {
                binding.tvUnreadCount.visibility = View.GONE
                Log.d("UsersAdapter", "tvUnreadCount GONE for ${user.name}")
            }

            // Agar userda rasm url bo'lsa, yuklab ko'rsatamiz, aks holda placeholder
            Glide.with(binding.root.context)
                    .load(
                    user.imageUrl ?: "https://cdn-icons-png.flaticon.com/512/634/634742.png"
                ) // imageUrl maydoni bo'lishi kerak User class'da, agar yo'q bo'lsa null yoki bo'sh
                .placeholder(com.samurai.chatme.R.drawable.img_1)
                .error(com.samurai.chatme.R.drawable.img_1)
                .circleCrop()
                .into(binding.ivUserImage)

            binding.root.setOnClickListener {
                onItemClick(user)
            }


        }

        fun clear() {
            binding.tvUnreadCount.text = ""
            binding.tvUnreadCount.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUsersBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun onViewRecycled(holder: UserViewHolder) {
        super.onViewRecycled(holder)
        holder.clear() // View qayta ishlatilganda tozalash
    }
}

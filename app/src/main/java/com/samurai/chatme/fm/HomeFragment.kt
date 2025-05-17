package com.samurai.chatme.fm

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.samurai.chatme.MessageModel
import com.samurai.chatme.User
import com.samurai.chatme.adapter.UsersAdapter
import com.samurai.chatme.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private val userList = ArrayList<User>()
    private lateinit var adapter: UsersAdapter
    private val dbRef = FirebaseDatabase.getInstance().getReference("users")
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
    private var doubleBackToExitPressedOnce = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = UsersAdapter(userList) { user ->
            val action = HomeFragmentDirections
                .actionHomeFragmentToWriteFragment(
                    userId = user.uid,
                    userName = user.name,
                    imageUrl = user.imageUrl!!
                )
            findNavController().navigate(action)
        }
        binding.rvUsers.adapter = adapter

        fetchUsers()
        fetchCurrentUser()





        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (doubleBackToExitPressedOnce) {
                requireActivity().finish()
            } else {
                doubleBackToExitPressedOnce = true
                Toast.makeText(
                    requireContext(),
                    "Chiqish uchun yana bir bor bosing",
                    Toast.LENGTH_SHORT
                ).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    doubleBackToExitPressedOnce = false
                }, 2000)
            }
        }


        binding.myAvatar.setOnClickListener {
            findNavController().navigate(
                HomeFragmentDirections.actionHomeFragmentToProfileFragment()
            )
        }


    }

    private fun fetchUsers() {
        binding.progress.visibility = View.VISIBLE
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val chatsRef = FirebaseDatabase.getInstance().getReference("chats")
        chatsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(chatSnapshot: DataSnapshot) {
                val unreadMap: MutableMap<String, Int> = mutableMapOf()
                // Har bir foydalanuvchi uchun boshlang‘ich qiymatni 0 deb o‘rnatamiz
                dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        for (snap in snapshot.children) {
                            val user = snap.getValue(User::class.java)
                            if (user != null && user.uid != currentUid) {
                                unreadMap[user.uid] = 0 // Boshlang‘ich qiymat 0
                            }
                        }

                        // Xabarlarni skan qilamiz
                        for (chatRoom in chatSnapshot.children) {
                            val chatRoomId = chatRoom.key
                            for (msgSnap in chatRoom.children) {
                                val lastMessageSnap = chatRoom.children.last()
                                val message = lastMessageSnap.getValue(MessageModel::class.java)

                                if (message != null && message.receiverId == currentUid) {
                                    val expectedChatRoomId =
                                        getChatRoomId(message.senderId, message.receiverId)
                                    if (chatRoomId == expectedChatRoomId) {
                                        if (message.isSeen) {
                                            // Agar isSeen true bo‘lsa, unreadMap ni 0 ga tenglashtiramiz
                                            unreadMap[message.senderId] = 0
                                            Log.d(
                                                "HomeFragment",
                                                "Message isSeen: true for senderId: ${message.senderId}, set unreadCount to 0"
                                            )
                                        } else {
                                            // Agar isSeen false bo‘lsa, unreadCount ni oshiramiz
                                            unreadMap[message.senderId] =
                                                (unreadMap[message.senderId] ?: 0) + 1
                                            Log.d(
                                                "HomeFragment",
                                                "Message isSeen: false for senderId: ${message.senderId}, unreadCount: ${unreadMap[message.senderId]}"
                                            )
                                           }
                                    }
                                }
                            }
                        }
                        Log.d("HomeFragment", "Unread counts: $unreadMap")

                        // Foydalanuvchilar ro‘yxatini yangilaymiz
                        userList.clear()
                        for (snap in snapshot.children) {
                            val user = snap.getValue(User::class.java)
                            if (user != null && user.uid != currentUid) {
                                user.unreadCount = unreadMap[user.uid] ?: 0
                                userList.add(user)
                                Log.d(
                                    "HomeFragment",
                                    "User: ${user.name}, UnreadCount: ${user.unreadCount}"
                                )
                            }
                        }
                        binding.rvUsers.adapter = adapter // Adapter ni qayta o‘rnatamiz
                        adapter.notifyDataSetChanged()
                        binding.progress.visibility = View.GONE
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.e("HomeFragment", "Error fetching users: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error fetching chats: ${error.message}")
            }
        })
    }

    private fun getChatRoomId(senderId: String, receiverId: String): String {
        return if (senderId < receiverId) senderId + receiverId
        else receiverId + senderId
    }

    override fun onResume() {
        super.onResume()
        fetchUsers() // HomeFragment ga qaytganda qayta yuklaymiz
    }

    private fun fetchCurrentUser() {
        if (currentUserUid == null) return
        dbRef.child(currentUserUid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentUser = snapshot.getValue(User::class.java)
                currentUser?.let {
                    binding.myName.text = it.name


                    Glide.with(binding.myAvatar.context)
                        .load(
                            it.imageUrl ?: "https://cdn-icons-png.flaticon.com/512/634/634742.png"
                        )
                        .circleCrop()
                        .into(binding.myAvatar)
                }
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }
}

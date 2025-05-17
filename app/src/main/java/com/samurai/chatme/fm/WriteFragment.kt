package com.samurai.chatme.fm

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.samurai.chatme.MessageModel
import com.samurai.chatme.R
import com.samurai.chatme.adapter.ChatAdapter
import com.samurai.chatme.databinding.FragmentWriteBinding


class WriteFragment : Fragment() {

    private var _binding: FragmentWriteBinding? = null

    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<MessageModel>()

    private lateinit var database: DatabaseReference
    private lateinit var senderId: String
    private lateinit var receiverId: String
    private lateinit var receiverName: String
    private lateinit var receiverPhoto: String
    private lateinit var chatRoomId: String
    private lateinit var currentUserName: String
    private var editingMessage: MessageModel? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWriteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // SafeArgs orqali qiymatlarni olish

        val args = WriteFragmentArgs.fromBundle(requireArguments())
        receiverId = args.userId
        receiverName = args.userName
        receiverPhoto = args.imageUrl

        senderId = FirebaseAuth.getInstance().uid ?: return
        database = FirebaseDatabase.getInstance().reference

        // Foydalanuvchining ismini olish
        database.child("users").child(senderId).child("name").get().addOnSuccessListener {
            currentUserName = it.value.toString()
        }

        // Chat ID (tartibli bo‘lishi uchun ikkala ID ni birlashtiramiz)
        chatRoomId = if (senderId < receiverId) senderId + receiverId
        else receiverId + senderId

        chatAdapter = ChatAdapter(messages, senderId) { view, position, message ->
            showPopupMenu(view, position, message)
        }
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatAdapter
        }


        markMessagesAsSeen()
        // Real-time xabarlarni tinglash
        listenForMessages()

        // Send Button
        binding.btnSend.setOnClickListener {
            val newText = binding.edtMessage.text.toString().trim()
            if (newText.isEmpty()) return@setOnClickListener

            if (editingMessage != null) {
                // Tahrirlash rejimi
                val targetMessage = editingMessage!!
                database.child("chats").child(chatRoomId).get()
                    .addOnSuccessListener { snapshot ->
                        for (child in snapshot.children) {
                            val msg = child.getValue(MessageModel::class.java)
                            if (msg?.message == targetMessage.message && (msg.senderId == targetMessage.senderId)) {
                                // Yangilaymiz
                                child.ref.child("message").setValue(newText)
                                    .addOnSuccessListener {
                                        editingMessage = null
                                        binding.edtMessage.setText("")
                                        binding.edtMessage.clearFocus()
                                    }
                                break
                            }
                        }
                    }
            } else {
                // Yangi xabar yuborish logikasi
                val m = MessageModel(
                    message = newText,
                    senderId = senderId,
                    receiverId = receiverId,
                    senderName = currentUserName,
                    isSeen = false// yoki sizda mavjud bo‘lsa, o‘zingizning qiymat
                )
                database.child("chats").child(chatRoomId).push().setValue(m)
                binding.edtMessage.setText("")
                binding.edtMessage.clearFocus()
            }
        }



        binding.name.text = receiverName
        Glide.with(requireContext()).load(receiverPhoto).into(binding.image)

        binding.chatRecyclerView.addOnLayoutChangeListener { _, _, _, _, bottom, _, _, _, oldBottom ->
            if (bottom < oldBottom && chatAdapter.itemCount > 0) {
                binding.chatRecyclerView.postDelayed({
                    binding.chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }, 100)
            }
        }
    }

    private fun showPopupMenu(view: View, position: Int, message: MessageModel) {
        val popup = androidx.appcompat.widget.PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.message_popup_menu, popup.menu)


        for (i in 0 until popup.menu.size()) {
            val item = popup.menu.getItem(i)
            val icon = item.icon
            icon?.mutate()?.setTint(ContextCompat.getColor(requireContext(), R.color.black))
            item.icon = icon
        }

        try {
            val fields = popup.javaClass.declaredFields
            for (field in fields) {
                if (field.name == "mPopup") {
                    field.isAccessible = true
                    val menuPopupHelper = field.get(popup)
                    val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                    val setForceIcons =
                        classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
                    setForceIcons.invoke(menuPopupHelper, true)
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_edit -> {
                    editMessage(message)
                    true
                }

                R.id.share_message -> {
                    shareText(message.message)
                    true
                }


                R.id.copy_message -> {
                    copyToClipboard(message.message, context)
                    true
                }

                R.id.menu_delete -> {
                    val context = requireContext()
                    val dialogView =
                        LayoutInflater.from(context).inflate(R.layout.dialog_delete, null)

                    val checkbox = dialogView.findViewById<CheckBox>(R.id.checkbox_delete_both)
                    val cancelText = dialogView.findViewById<TextView>(R.id.bekor)
                    val deleteText = dialogView.findViewById<TextView>(R.id.och)

                    val dialog =
                        AlertDialog.Builder(context).setView(dialogView).setCancelable(true)
                            .create()

                    cancelText.setOnClickListener {
                        dialog.dismiss()
                    }

                    deleteText.setOnClickListener {
                        val deleteForBoth = checkbox.isChecked
                        dialog.dismiss()

                        // Bazadan xabarni topib, ID orqali o‘chirish
                        database.child("chats").child(chatRoomId).get()
                            .addOnSuccessListener { snapshot ->
                                for (child in snapshot.children) {
                                    val msg = child.getValue(MessageModel::class.java)
                                    if (msg?.message == message.message && msg.senderId == message.senderId) {
                                        if (deleteForBoth) {
                                            // Ikkala foydalanuvchi uchun o‘chirish
                                            child.ref.removeValue().addOnSuccessListener {
                                                chatAdapter.removeMessage(position)
                                            }
                                        } else {
                                            if (msg?.message?.trim() == message.message.trim() && msg.senderId == message.senderId) {
                                                child.ref.removeValue().addOnSuccessListener {
                                                    chatAdapter.removeMessage(position)
                                                }
                                            }
                                        }
                                        break
                                    }
                                }
                            }
                    }
                    dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    dialog.show()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun listenForMessages() {
        database.child("chats").child(chatRoomId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return

                    messages.clear()
                    for (child in snapshot.children) {
                        val message = child.getValue(MessageModel::class.java)
                        if (message != null) {
                            messages.add(message)
                        }
                    }

                    chatAdapter.notifyDataSetChanged()

                    if (messages.isNotEmpty()) {
                        binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("WriteFragment", "Error listening for messages: ${error.message}")
                }
            })
    }

    private fun editMessage(message: MessageModel) {
        editingMessage = message
        binding.edtMessage.setText(message.message)
        binding.edtMessage.setSelection(message.message.length)
        binding.edtMessage.requestFocus()

        val imm =
            requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.edtMessage, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun copyToClipboard(text: String, context: Context?) {
        val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Copied Message", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Xabar nusxalandi", Toast.LENGTH_SHORT).show()
    }

    private fun markMessagesAsSeen() {
        val chatRef = database.child("chats").child(chatRoomId)
        chatRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (msgSnap in snapshot.children) {
                    val message = msgSnap.getValue(MessageModel::class.java) ?: continue
                    if (message.receiverId == senderId && !message.isSeen) {
                        msgSnap.ref.child("isSeen").setValue(true)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("WriteFragment", "Failed to mark messages as seen: ${error.message}")
            }
        })
    }

    private fun shareText(text: String) {
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        val shareIntent = android.content.Intent.createChooser(sendIntent, "Xabarni ulashish")
        startActivity(shareIntent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


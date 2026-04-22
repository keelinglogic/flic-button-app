package uk.co.sixpillar.flicbridge.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.flic.flic2libandroid.Flic2Button
import uk.co.sixpillar.flicbridge.databinding.ItemButtonBinding

class ButtonAdapter(
    private val onButtonClick: (Flic2Button) -> Unit
) : ListAdapter<Flic2Button, ButtonAdapter.ButtonViewHolder>(ButtonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ButtonViewHolder {
        val binding = ItemButtonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ButtonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ButtonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ButtonViewHolder(
        private val binding: ItemButtonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(button: Flic2Button) {
            binding.txtButtonName.text = button.name ?: "Unnamed Button"
            binding.txtButtonAddress.text = button.bdAddr

            val connectionState = when (button.connectionState) {
                Flic2Button.CONNECTION_STATE_DISCONNECTED -> "Disconnected"
                Flic2Button.CONNECTION_STATE_CONNECTING -> "Connecting..."
                Flic2Button.CONNECTION_STATE_CONNECTED_STARTING -> "Starting..."
                Flic2Button.CONNECTION_STATE_CONNECTED_READY -> "Connected"
                else -> "Unknown"
            }

            val battery = button.lastKnownBatteryLevel?.estimatedPercentage ?: "?"
            binding.txtButtonStatus.text = "$connectionState | Battery: $battery%"

            binding.root.setOnClickListener {
                onButtonClick(button)
            }
        }
    }

    class ButtonDiffCallback : DiffUtil.ItemCallback<Flic2Button>() {
        override fun areItemsTheSame(oldItem: Flic2Button, newItem: Flic2Button): Boolean {
            return oldItem.bdAddr == newItem.bdAddr
        }

        override fun areContentsTheSame(oldItem: Flic2Button, newItem: Flic2Button): Boolean {
            return oldItem.bdAddr == newItem.bdAddr &&
                    oldItem.connectionState == newItem.connectionState
        }
    }
}

package cz.cvut.isc.tripregistrator.screen.main

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import cz.cvut.isc.tripregistrator.R
import cz.cvut.isc.tripregistrator.model.Trip
import kotlinx.android.synthetic.main.trip_card.view.*
import java.text.SimpleDateFormat

/**
 * @author David Khol
 * @since 26.08.2018
 */
class TripsAdapter(
		private val registerTrip: (Trip) -> Unit,
		private val unregisterTrip: (Trip) -> Unit
) : RecyclerView.Adapter<TripsAdapter.ViewHolder>() {

	var trips: List<Trip> = emptyList()
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	var currentlyLoadingTrip: Trip? = null
		set(value) {
			field = value
			notifyDataSetChanged()
		}

	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		val view = LayoutInflater.from(parent.context).inflate(R.layout.trip_card, parent, false)
		return ViewHolder(view)
	}

	override fun getItemCount(): Int {
		return trips.size
	}

	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		val isLoading = trips[position].id == currentlyLoadingTrip?.id
		holder.bind(trips[position], registerTrip, unregisterTrip, isLoading)
	}

	override fun onViewRecycled(holder: ViewHolder) {
		holder.unbind()
	}

	class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {

		companion object {
			@SuppressLint("SimpleDateFormat")
			private val dateFormat = SimpleDateFormat("dd MMM")
			@SuppressLint("SimpleDateFormat")
			private val timeFormat = SimpleDateFormat("HH:mm")
		}

		private val context = view.context

		fun bind(trip: Trip, registerTrip: (Trip) -> Unit, unregisterTrip: (Trip) -> Unit, isLoading: Boolean) {
			view.txt_name.text = trip.name
			view.txt_date_from.text = trip.dateFrom?.let { dateFormat.format(it) }
			view.txt_date_to.text = trip.dateTo?.let { dateFormat.format(it) }
			view.txt_time_from.text = trip.dateFrom?.let { timeFormat.format(it) }
			view.txt_time_to.text = trip.dateTo?.let { timeFormat.format(it) }
			view.txt_price.text = context.getString(R.string.price_czk, trip.price)
			view.txt_capacity.text = context.getString(R.string.capacity,
					trip.participants,
					trip.capacity ?: context.getString(R.string.unlimited)
			)
			view.progress.visibility = if (isLoading) View.VISIBLE else View.INVISIBLE
			view.btn_register.apply {
				visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
				text = context.getString(when {
					trip.isRegistered -> R.string.registered
					trip.isFull -> R.string.full
					else -> R.string.not_registered
				})

				ViewCompat.setBackgroundTintList(this, ColorStateList.valueOf(ContextCompat.getColor(context,
						when {
							trip.isRegistered -> R.color.trip_registered
							trip.isFull -> R.color.trip_full
							else -> R.color.trip_not_registered
						})))

				setOnClickListener {
					if (trip.isRegistered) {
						unregisterTrip(trip)
					} else {
						registerTrip(trip)
					}
				}
			}
		}

		fun unbind() {
			view.btn_register.setOnClickListener(null)
		}

	}

}

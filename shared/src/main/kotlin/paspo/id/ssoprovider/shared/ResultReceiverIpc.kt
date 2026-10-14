package paspo.id.ssoprovider.shared

import android.os.Parcel
import android.os.ResultReceiver

@PaspoInternalApi
public fun ResultReceiver.toIpcReceiver(): ResultReceiver {
    val parcel = Parcel.obtain()
    return try {
        writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        ResultReceiver.CREATOR.createFromParcel(parcel)
    } finally {
        parcel.recycle()
    }
}

package org.videolan.vlc.gui.onboarding

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.visualizer.amplitude.AudioRecordView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import org.videolan.vlc.R

private const val TAG = "OnboardingShadowingFrag"
class OnboardingShadowingFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_onboarding_shadowing, container, false)
    }

    val dummyAmplitude = listOf(0, 69, 1117, 11056, 13691, 8533, 4239, 363, 137, 472, 1239, 139, 159, 75, 6674, 8290, 5757, 7161, 6263, 2446, 1599, 8188, 6501, 6536, 1278, 4891, 3409, 3242, 3863, 3242, 3792, 2070, 1959, 9588, 7078, 11731, 10186, 2842, 7117, 8159, 5865, 2139, 453, 10796, 8468, 4445, 6321, 5488, 11299, 4809, 1976, 8468, 565, 3284, 8543, 6157, 5325, 2683, 11148, 4579, 1992, 861, 580, 1252, 616, 573, 464, 1310, 6219, 9863, 8905, 6984, 917, 216, 254, 1935, 5237, 4917, 6634, 5129, 4796, 6241, 7372, 6877, 1324, 3034, 6649, 3157, 688, 15398, 5646, 4002, 3870, 3208, 1694, 3111, 5054, 5248, 3053, 7529, 5376, 3317, 2079, 977, 5779, 3435, 4048, 5543, 5770, 2308, 1761, 2359, 3608, 2112, 1240, 4471, 999, 698, 532, 4100, 447, 3296, 8575, 3129, 590, 1263, 4383, 2117, 1417, 4797, 5307, 5450, 4558, 2898, 653, 3789, 2354, 612, 2917, 2341, 503, 3063, 3823, 1178, 1100, 4269, 1057, 535, 3314, 6832, 2107, 3158, 3950, 1864, 2773, 1351, 1281, 7215, 1899, 3086, 2600, 2200, 3078, 5989, 6447, 4414, 3436, 4054, 4199, 5299, 2649, 246, 981, 2407, 478, 237, 155, 94, 72, 64, 65, 71, 75, 1071, 7169, 10603, 10597, 3634, 6196, 8853, 7276, 8691, 6365, 5543, 6616, 5863, 6970, 7400, 5784, 517, 115, 6737, 9624, 6536, 550, 128, 76, 83, 253, 0, 0, 0, 0, 981, 391, 475, 940, 10933, 12516, 6310, 6229, 6377, 6492, 1651, 13840, 10185, 5559, 7339, 2756, 15351, 13192, 8765, 3111, 14573, 9439, 6644, 6459, 5797, 3891, 4871, 5458, 3086, 5394, 961, 8116, 5571, 9568, 1535, 2181, 6091, 7962, 4905, 6261, 1732, 10829, 6841, 6246, 3865, 536, 537, 749, 8917, 6866, 9581, 7316, 5421, 4485, 11614, 8297, 6560, 5845, 3351, 1990, 5100, 3519, 995, 8117, 11174, 7228, 5346, 5046, 2796, 9777, 9599, 3548, 6133, 4323, 8004, 9634, 6011, 10375, 10977, 8812, 7451, 5739, 4526, 4920, 3632, 7139, 3911, 2599, 5059, 5052, 8500, 6639, 3922, 3161, 12730, 6551, 812, 3430, 3315, 3878, 4666, 2709, 11307, 4241, 3734, 7846, 7915, 8751, 927, 515, 305, 5943, 16275, 7883, 1275, 6656, 4963, 4168, 3177, 8123, 9185, 8223, 8303, 5372, 5151, 7755, 14102, 3417, 6446, 7360, 6160, 3301, 3392, 2195, 8108, 5432, 9011, 7289, 3017, 14732, 17188, 16499, 9220, 5368, 7476, 470, 1073, 571, 625, 599, 11819, 13359, 14010, 5649, 9374, 15019, 10589, 7394, 10757, 11159, 6275, 4042, 3841, 5634, 4985, 4815, 1748, 11325, 2661, 14955, 11463, 699, 468, 303, 86, 16343, 17230, 14605, 9577, 10907, 3880, 14545, 7543, 8867, 15024, 5944, 7756, 2220, 6299, 5483, 997, 5821, 5570, 2095, 2493, 8018, 9501, 6239, 343, 629, 5040, 4838, 3980, 5446, 350, 1180, 19721, 20168, 4942, 6076, 6692, 10990, 6010, 6597, 3639, 69, 9056, 15256, 991, 5386, 776, 2450, 11822, 6649, 8485, 12227, 965, 556, 768, 449, 3265, 16103, 14370, 8068, 16041, 6392, 11089, 13148, 9744, 8174, 7749, 11464, 5963, 5382, 4280, 4897, 1812, 4704, 4974, 13143, 11121, 14038, 8345, 7559, 3850, 20017, 17553, 1677, 1887, 18258, 18336, 13494, 2804, 1236, 375, 476, 669, 444, 16535, 10533, 12294, 5663, 6027, 7955, 5582, 3755, 6859, 2493, 5824, 6811, 6896, 6712, 6768, 7709, 8050, 6584, 5488, 5084, 5747, 5779, 5513, 4963, 6539, 6636, 5782, 2341, 200, 93, 93, 88, 868)
    var i = 0
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val amplitudeView =view.findViewById<AudioRecordView>(R.id.audio_amplitude)
        val tickerChannel = ticker(delayMillis = 100, initialDelayMillis = 0)
        lifecycleScope.launch(Dispatchers.Main) {
            for (event in tickerChannel) {
                if (i == dummyAmplitude.size) i = 0
                amplitudeView.update(dummyAmplitude[i++])
            }
        }
    }

    companion object {
        fun newInstance() = OnboardingShadowingFragment()
    }
}
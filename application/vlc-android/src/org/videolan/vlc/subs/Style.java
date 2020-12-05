/*
 * This file is part of Butter.
 *
 * Butter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Butter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Butter. If not, see <http://www.gnu.org/licenses/>.
 */

package org.videolan.vlc.subs;

public class Style {

    private static int styleCounter;

    /**
     * Constructor that receives a String to use a its identifier
     *
     * @param styleName = identifier of this style
     */
    protected Style(String styleName) {
        this.iD = styleName;
    }

    /**
     * Constructor that receives a String with the new styleName and a style to copy
     *
     * @param styleName
     * @param style
     */
    protected Style(String styleName, Style style) {
        this.iD = styleName;
        this.font = style.font;
        this.fontSize = style.fontSize;
        this.color = style.color;
        this.backgroundColor = style.backgroundColor;
        this.textAlign = style.textAlign;
        this.italic = style.italic;
        this.underline = style.underline;
        this.bold = style.bold;

    }

    /* ATTRIBUTES */
    protected String iD;
    protected String font;
    protected String fontSize;
    /**
     * colors are stored as 8 chars long RGBA
     */
    protected String color;
    protected String backgroundColor;
    protected String textAlign = "";

    protected boolean italic;
    protected boolean bold;
    protected boolean underline;

	/* METHODS */

    /**
     * To get the string containing the hex value to put into color or background color
     *
     * @param format supported: "name", "&HBBGGRR", "&HAABBGGRR", "decimalCodedBBGGRR", "decimalCodedAABBGGRR"
     * @param value  RRGGBBAA string
     * @return
     */
    protected static String getRGBValue(String format, String value) {
        String color = null;
        try {
            if (format.equalsIgnoreCase("name")) {
                //standard color format from W3C
                if (value.equals("transparent"))
                    color = "00000000";
                else if (value.equals("black"))
                    color = "ff000000";
                else if (value.equals("silver"))
                    color = "ffc0c0c0";
                else if (value.equals("gray"))
                    color = "ff808080";
                else if (value.equals("white"))
                    color = "ffffffff";
                else if (value.equals("maroon"))
                    color = "ff800000";
                else if (value.equals("red"))
                    color = "ffff0000";
                else if (value.equals("purple"))
                    color = "ff800080";
                else if (value.equals("fuchsia"))
                    color = "ffff00ff";
                else if (value.equals("magenta"))
                    color = "ffff00ff ";
                else if (value.equals("green"))
                    color = "ff008000";
                else if (value.equals("lime"))
                    color = "ff00ff00";
                else if (value.equals("olive"))
                    color = "ff808000";
                else if (value.equals("yellow"))
                    color = "ffffff00";
                else if (value.equals("navy"))
                    color = "ff000080";
                else if (value.equals("blue"))
                    color = "ff0000ff";
                else if (value.equals("teal"))
                    color = "ff008080";
                else if (value.equals("aqua"))
                    color = "ff00ffff";
                else if (value.equals("cyan"))
                    color = "ff00ffff";
            } else if (format.equalsIgnoreCase("&HBBGGRR")) {
                //hex format from SSA
                //FFMPEG generates "&H0" and "&HF"
                while (value.length() < 8) {
                    value += value.charAt(value.length()-1);
                }
                StringBuilder sb = new StringBuilder();
                sb.append(value.substring(6));
                sb.append(value.substring(4, 6));
                sb.append(value.substring(2, 4));
                sb.append("ff");
                color = sb.toString();
            } else if (format.equalsIgnoreCase("&HAABBGGRR")) {
                //hex format from ASS
                //FFMPEG generates "&H0" and "&HF"
                while (value.length() < 10) {
                    value += value.charAt(value.length()-1);
                }
                StringBuilder sb = new StringBuilder();
                sb.append(value.substring(8));
                sb.append(value.substring(6, 8));
                sb.append(value.substring(4, 6));
                sb.append(value.substring(2, 4));
                color = sb.toString();
            } else if (format.equalsIgnoreCase("decimalCodedBBGGRR")) {
                //normal format from SSA
                color = Integer.toHexString(Integer.parseInt(value));
                //any missing 0s are filled in
                while (color.length() < 6) color = "0" + color;
                //order is reversed
                color = color.substring(4) + color.substring(2, 4) + color.substring(0, 2) + "ff";
            } else if (format.equalsIgnoreCase("decimalCodedAABBGGRR")) {
                //normal format from ASS
                color = Long.toHexString(Long.parseLong(value));
                //any missing 0s are filled in
                while (color.length() < 8) color = "0" + color;
                //order is reversed
                color = color.substring(6) + color.substring(4, 6) + color.substring(2, 4) + color.substring(0, 2);
            }
            return color;
        } catch (Exception e) {
           return  "ff000000";
        }
    }

    protected static String defaultID() {
        return "default" + styleCounter++;
    }

    @Override
    public String toString() {
        return "Style{" +
                "id='" + iD + '\'' +
                ", font='" + font + '\'' +
                ", fontSize='" + fontSize + '\'' +
                ", color='" + color + '\'' +
                ", backgroundColor='" + backgroundColor + '\'' +
                ", textAlign='" + textAlign + '\'' +
                ", italic=" + italic +
                ", bold=" + bold +
                ", underline=" + underline +
                '}';
    }

}

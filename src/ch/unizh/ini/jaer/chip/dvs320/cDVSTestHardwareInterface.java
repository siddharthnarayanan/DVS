/*
 * CypressFX2Biasgen.java
 *
 * Created on 23 Jan 2008
 *
 */
package ch.unizh.ini.jaer.chip.dvs320;

import ch.unizh.ini.jaer.chip.util.scanner.ScannerHardwareInterface;
import net.sf.jaer.biasgen.Biasgen;
import net.sf.jaer.aemonitor.AEPacketRaw;
import net.sf.jaer.hardwareinterface.HardwareInterfaceException;
import net.sf.jaer.hardwareinterface.usb.cypressfx2.CypressFX2;
import net.sf.jaer.hardwareinterface.usb.cypressfx2.CypressFX2Biasgen;
import de.thesycon.usbio.*;
import de.thesycon.usbio.structs.*;
import javax.swing.ProgressMonitor;
import java.io.*;
import java.math.BigInteger;
import java.util.prefs.Preferences;

/**
 * Adds functionality of cDVSTest retina test chips to base classes for Cypress FX2 interface.
 *
 * @author tobi
 */
public class cDVSTestHardwareInterface extends CypressFX2Biasgen implements  cDVSTestADCHardwareInterface, ScannerHardwareInterface {

    static Preferences cdvshwprefs = Preferences.userNodeForPackage(cDVSTestHardwareInterface.class); // TODO should really come from Chip instance, not this class
    /** The USB product ID of this device */
    static public final short PID = (short) 0x840A;
    private boolean adcEnabled = cdvshwprefs.getBoolean("cDVSTestHardwareInterface.adcEnabled", true);
    private int TrackTime =  cdvshwprefs.getInt("cDVSTestHardwareInterface.TrackTime", 50),
            RefOnTime = cdvshwprefs.getInt("cDVSTestHardwareInterface.RefOnTime", 20),
            RefOffTime =  cdvshwprefs.getInt("cDVSTestHardwareInterface.RefOffTime", 20),
            IdleTime =  cdvshwprefs.getInt("cDVSTestHardwareInterface.IdleTime", 10);
    private boolean Select5Tbuffer = cdvshwprefs.getBoolean("cDVSTestHardwareInterface.Select5Tbuffer", true);
    private boolean UseCalibration = cdvshwprefs.getBoolean("cDVSTestHardwareInterface.UseCalibration", false);
    private boolean scanContinuouslyEnabled = cdvshwprefs.getBoolean("cDVSTestHardwareInterface.scanContinuouslyEnabled", true);
    private int scanX = cdvshwprefs.getInt("cDVSTestHardwareInterface.scanX", 0);
    private int scanY = cdvshwprefs.getInt("cDVSTestHardwareInterface.scanY", 0);
    private int ADCchannel = cdvshwprefs.getInt("cDVSTestHardwareInterface.ADCchannel", 3);
    private static final int ADCchannelshift = 5;
    private static final short ADCconfig = (short) 0x100;   //normal power mode, single ended, sequencer unused : (short) 0x908;
    private final static short ADCconfigLength = (short) 12;

    public static final String
            EVENT_SELECT_5T_BUFFER="Select5Tbuffer",
            EVENT_USE_CALIBRATION="UseCalibration";

    /** Creates a new instance of CypressFX2Biasgen */
    public cDVSTestHardwareInterface(int devNumber) {
        super(devNumber);
    }

    @Override
    public void open() throws HardwareInterfaceException {
        super.open();
        sendADCConfiguration();
    }


    /** Overrides sendConfiguration to use this bias generator to format the data and send the data.
     * <p>
     * Data is sent in bytes. Each byte is loaded into the shift register in big-endian bit order, starting with the msb and ending with the lsb.
     * Bytes are loaded starting with the first byte from formatConfigurationBytes (element 0). Therefore the last bit in the on-chip shift register (the one
     * that is furthest away from the bit input pin) should be in the msb of the first byte returned by formatConfigurationBytes.
     * 
     * @param biasgen the DVS320 biasgen which knows how to format the bias and bit configuration.
     * @throws net.sf.jaer.hardwareinterface.HardwareInterfaceException
     */
    @Override
    public synchronized void sendConfiguration(Biasgen biasgen) throws HardwareInterfaceException {
        byte[] bytes = biasgen.formatConfigurationBytes(biasgen);
        if (bytes == null) {
            log.warning("null byte array - not sending");
            return;
        }
        super.sendBiasBytes(bytes);
    }

    public void setTrackTime(int trackTimeUs) {
        try {
            int old=this.TrackTime;
            TrackTime = trackTimeUs;  // TODO bound values here
            cdvshwprefs.putInt("cDVSTestHardwareInterface.TrackTime", TrackTime);
            getSupport().firePropertyChange(EVENT_TRACK_TIME,old,TrackTime);
            sendADCConfiguration();
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    public void setIdleTime(int trackTimeUs) {
        try {
             int old=this.IdleTime;
           IdleTime = trackTimeUs;// TODO bound values here
            cdvshwprefs.putInt("cDVSTestHardwareInterface.IdleTime", IdleTime);
            getSupport().firePropertyChange(EVENT_IDLE_TIME,old,IdleTime);
            sendADCConfiguration();
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    public void setRefOnTime(int trackTimeUs) {
        try {
            int old=this.RefOnTime;
            RefOnTime = trackTimeUs;// TODO bound values here
            sendADCConfiguration();
            cdvshwprefs.putInt("cDVSTestHardwareInterface.RefOnTime", RefOnTime);
            getSupport().firePropertyChange(EVENT_REF_ON_TIME,old,RefOnTime);
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    public void setRefOffTime(int trackTimeUs) {
        try {
            int old=this.RefOffTime;
            RefOffTime = trackTimeUs;// TODO bound values here
            cdvshwprefs.putInt("cDVSTestHardwareInterface.RefOffTime", RefOffTime);
            getSupport().firePropertyChange(EVENT_REF_OFF_TIME,old,RefOffTime);
            sendADCConfiguration();
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    public void setSelect5Tbuffer(boolean se) {
        try {
            boolean old=this.Select5Tbuffer;
            Select5Tbuffer = se;
            cdvshwprefs.putBoolean("cDVSTestHardwareInterface.Select5Tbuffer", Select5Tbuffer);
            getSupport().firePropertyChange(EVENT_SELECT_5T_BUFFER, old, Select5Tbuffer);
            sendADCConfiguration();
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    public void setUseCalibration(boolean se) {
        try {
           boolean old=this.UseCalibration;
             UseCalibration = se;
            cdvshwprefs.putBoolean("cDVSTestHardwareInterface.UseCalibration", UseCalibration);
          getSupport().firePropertyChange(EVENT_USE_CALIBRATION, old, UseCalibration);
             sendADCConfiguration();
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    /** On the cDVSTest chips, the channel 'mask' is actually just the single channel that is digitized, not a mask for desired channels. 
     * 
     * @param chan the channel number, ranging 0-3
     */
    @Override
    public void setADCChannel(int chan) {// TODO fix to proper mask with labeled ADC samples in returned data
        try {
            int old=this.ADCchannel;
            if (chan < 0) {
                chan = 0;
            } else if (chan > 3) {
                chan = 3;
            }
            ADCchannel = chan;
            cdvshwprefs.putInt("cDVSTestHardwareInterface.ADCchannel", ADCchannel);
         getSupport().firePropertyChange(EVENT_ADC_CHANNEL_MASK, old, ADCchannel);
             sendADCConfiguration();
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    /**
     * @return the scanContinuouslyEnabled
     */
    public boolean isScanContinuouslyEnabled() {
        return scanContinuouslyEnabled;
    }

    /**
     * @param scanContinuouslyEnabled the scanContinuouslyEnabled to set
     */
    public void setScanContinuouslyEnabled(boolean scanContinuouslyEnabled) {
        try {
            boolean old=this.scanContinuouslyEnabled;
            this.scanContinuouslyEnabled = scanContinuouslyEnabled;
            cdvshwprefs.putBoolean("cDVSTestHardwareInterface.scanContinuouslyEnabled", scanContinuouslyEnabled);
            getSupport().firePropertyChange(EVENT_SCAN_CONTINUOUSLY_ENABLED, old, this.scanContinuouslyEnabled);
            sendADCConfiguration();
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    /**
     * @return the scanX
     */
    public int getScanX() {
        return scanX;
    }

    public int getScanSizeX() {
        return cDVSTest20.SIZE_X_CDVS;
    }

    public int getScanSizeY() {
        return cDVSTest20.SIZE_Y_CDVS;
    }


    /**
     * @param scanX the scanX to set
     */
    public void setScanX(int scanX) {
        int old=this.scanX;
        if (scanX < 0) {
            scanX = 0;
        } else if (scanX >= getScanSizeX()) {
            scanX = getScanSizeX() - 1;
        }
        try {
            this.scanX = scanX;
            cdvshwprefs.putInt("cDVSTestHardwareInterface.scanX", scanX);
            getSupport().firePropertyChange(EVENT_SCAN_X,old,this.scanX);
            sendADCConfiguration();
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    /**
     * @return the scanY
     */
    public int getScanY() {
        return scanY;
    }

    /**
     * @param scanY the scanY to set
     */
    public void setScanY(int scanY) {
       int old=this.scanY;
         if (scanY < 0) {
            scanY = 0;
        } else if (scanY >= getScanSizeY()) {
            scanY = getScanSizeY() - 1;
        }
        try {
            this.scanY = scanY;
            cdvshwprefs.putInt("cDVSTestHardwareInterface.scanY", scanY);
           getSupport().firePropertyChange(EVENT_SCAN_Y,old,this.scanY);
             sendADCConfiguration();
        } catch (HardwareInterfaceException ex) {
            log.warning(ex.toString());
        }
    }

    private String getBitString(short value, short nSrBits) {
        StringBuilder s = new StringBuilder();

        int k = nSrBits - 1;
        while (k >= 0) {
            int x = value & (1 << k); // start with msb
            boolean b = (x == 0); // get bit
            s.append(b ? '0' : '1'); // append to string 0 or 1, string grows with msb on left
            k--;
        } // construct big endian string e.g. code=14, s='1011'
        String bitString = s.toString();
        return bitString;
    }

    @Override
    synchronized public void sendADCConfiguration() throws HardwareInterfaceException {
        short ADCword = (short) (ADCconfig | (getADCChannel() << ADCchannelshift));

        int nBits = 0;

        StringBuilder s = new StringBuilder();

        // scanner control
        final int SCANXY_NBITS = 6;
        s.append(getBitString(isScanContinuouslyEnabled() ? (short) 1 : (short) 0, (short) 1));
        s.append(getBitString((short) getScanX(), (short) SCANXY_NBITS));
        s.append(getBitString((short) getScanY(), (short) SCANXY_NBITS));
        nBits+=1+2*SCANXY_NBITS;
        
        // ADC params
        s.append(getBitString((short) (getIdleTime() * 15), (short) 16)); // multiplication with 15 to get from us to clockcycles
        nBits += 16;
        s.append(getBitString((short) (getRefOffTime() * 15), (short) 16)); // multiplication with 15 to get from us to clockcycles
        nBits += 16;
        s.append(getBitString((short) (getRefOnTime() * 15), (short) 16)); // multiplication with 15 to get from us to clockcycles
        nBits += 16;
        s.append(getBitString((short) (getTrackTime() * 15), (short) 16)); // multiplication with 15 to get from us to clockcycles
        nBits += 16;
        s.append(getBitString(ADCword, ADCconfigLength));
        nBits += ADCconfigLength;

        // readout pathway
        if (isUseCalibration()) {
            s.append(getBitString((short) 1, (short) 1));
        } else {
            s.append(getBitString((short) 0, (short) 1));
        }
        nBits += 1;

        if (isSelect5Tbuffer()) {
            s.append(getBitString((short) 1, (short) 1));
        } else {
            s.append(getBitString((short) 0, (short) 1));
        }
        nBits += 1;

        //s.reverse();
        //System.out.println(s);

        BigInteger bi = new BigInteger(s.toString(), 2);
        byte[] byteArray = bi.toByteArray(); // finds minimal set of bytes in big endian format, with MSB as first element
        // we need to pad out to nbits worth of bytes
        int nbytes = (nBits % 8 == 0) ? (nBits / 8) : (nBits / 8 + 1); // 8->1, 9->2
        byte[] bytes = new byte[nbytes];
        System.arraycopy(byteArray, 0, bytes, nbytes - byteArray.length, byteArray.length);

        System.out.print(s.length()+" CPLD bits="+s+" bytes=");
        for(byte b:bytes){
            System.out.print(String.format("%2x ",b));
        }
        System.out.println("");
        this.sendVendorRequest(VENDOR_REQUEST_WRITE_CPLD_SR, (short) 0, (short) 0, bytes); // stops ADC running
        setADCEnabled(isADCEnabled());
    }

    synchronized public void startADC() throws HardwareInterfaceException {
        this.sendVendorRequest(VENDOR_REQUEST_RUN_ADC, (short) 1, (short) 0);
    }

    synchronized public void stopADC() throws HardwareInterfaceException {
        this.sendVendorRequest(VENDOR_REQUEST_RUN_ADC, (short) 0, (short) 0);
    }

    public boolean isADCEnabled() {
        return adcEnabled;
    }

    public void setADCEnabled(boolean yes) throws HardwareInterfaceException {
        boolean old=this.adcEnabled;
        this.adcEnabled = yes;
        cdvshwprefs.putBoolean("cDVSTestHardwareInterface.adcEnabled", yes);
        getSupport().firePropertyChange(EVENT_ADC_ENABLED, old, this.adcEnabled);
        if (yes) {
            startADC();
        } else {
            stopADC();
        }
    }

    @Override
    synchronized public void setPowerDown(boolean powerDown) throws HardwareInterfaceException {
        //        System.out.println("BiasgenUSBInterface.setPowerDown("+powerDown+")");
        //        if(!powerDown)
        //            setPowerDownSingle(true);
        setPowerDownSingle(powerDown);
    }

    synchronized private void setPowerDownSingle(final boolean powerDown) throws HardwareInterfaceException {

        if (gUsbIo == null) {
            throw new RuntimeException("device must be opened before sending this vendor request");
        }
        USBIO_CLASS_OR_VENDOR_REQUEST vendorRequest = new USBIO_CLASS_OR_VENDOR_REQUEST();
        int result;
        //        System.out.println("sending bias bytes");
        USBIO_DATA_BUFFER dataBuffer = new USBIO_DATA_BUFFER(0); // no data, control is in setupdat
        vendorRequest.Request = VENDOR_REQUEST_SET_ARRAY_RESET;
        vendorRequest.Type = UsbIoInterface.RequestTypeVendor;
        vendorRequest.Recipient = UsbIoInterface.RecipientDevice;
        vendorRequest.RequestTypeReservedBits = 0;
        vendorRequest.Index = 0;  // meaningless for this request

        vendorRequest.Value = (short) (powerDown ? 1 : 0);  // this is the request bit, if powerDown true, send value 1, false send value 0

        dataBuffer.setNumberOfBytesToTransfer(dataBuffer.Buffer().length);
        result = gUsbIo.classOrVendorOutRequest(dataBuffer, vendorRequest);
        if (result != de.thesycon.usbio.UsbIoErrorCodes.USBIO_ERR_SUCCESS) {
            throw new HardwareInterfaceException("setPowerDown: unable to send: " + UsbIo.errorText(result));
        }
        HardwareInterfaceException.clearException();

    }

    synchronized private void setChipReset(final boolean reset) throws HardwareInterfaceException {
        this.chipReset=reset;
        if (gUsbIo == null) {
            throw new RuntimeException("device must be opened before sending this vendor request");
        }
        USBIO_CLASS_OR_VENDOR_REQUEST vendorRequest = new USBIO_CLASS_OR_VENDOR_REQUEST();
        int result;
        //        System.out.println("sending bias bytes");
        USBIO_DATA_BUFFER dataBuffer = new USBIO_DATA_BUFFER(0); // no data, control is in setupdat
        vendorRequest.Request = VENDOR_REQUEST_SET_ARRAY_RESET;
        vendorRequest.Type = UsbIoInterface.RequestTypeVendor;
        vendorRequest.Recipient = UsbIoInterface.RecipientDevice;
        vendorRequest.RequestTypeReservedBits = 0;
        vendorRequest.Index = 0;  // meaningless for this request

        vendorRequest.Value = (short) (reset ? 1 : 0);  // this is the request bit, if powerDown true, send value 1, false send value 0

        dataBuffer.setNumberOfBytesToTransfer(dataBuffer.Buffer().length);
        result = gUsbIo.classOrVendorOutRequest(dataBuffer, vendorRequest);
        if (result != de.thesycon.usbio.UsbIoErrorCodes.USBIO_ERR_SUCCESS) {
            throw new HardwareInterfaceException("setChipReset: unable to send: " + UsbIo.errorText(result));
        }
        HardwareInterfaceException.clearException();
    }

    @Override
    synchronized public void resetTimestamps() {
        log.info(this + ".resetTimestamps(): zeroing timestamps");


        // send vendor request for device to reset timestamps
        if (gUsbIo == null) {
            throw new RuntimeException("device must be opened before sending this vendor request");
        }
        try {
            boolean adc=isADCEnabled();
            stopADC();
            setChipReset(!chipReset);
            if (adc) {
                    startADC();
            }
            this.sendVendorRequest(VENDOR_REQUEST_RESET_TIMESTAMPS);
        } catch (HardwareInterfaceException e) {
            log.warning("could not send vendor request to reset timestamps: " + e);
        }
    }
    private boolean chipReset = false;

    private byte[] parseHexData(String firmwareFile) throws IOException {

        byte[] fwBuffer;
        // load firmware file (this is binary file of 8051 firmware)

        log.info("reading firmware file " + firmwareFile);
        FileReader reader;
        LineNumberReader lineReader;
        String line;
        int length;
        // load firmware file (this is a lattice c file)
        try {

            reader = new FileReader(firmwareFile);
            lineReader = new LineNumberReader(reader);

            line = lineReader.readLine();
            while (!line.startsWith("xdata")) {
                line = lineReader.readLine();
            }
            int scIndex = line.indexOf(";");
            int eqIndex = line.indexOf("=");
            int index = 0;
            length = Integer.parseInt(line.substring(eqIndex + 2, scIndex));
            // log.info("File length: " + length);
            String[] tokens;
            fwBuffer = new byte[length];
            Short value;
            while (!line.endsWith("};")) {
                line = lineReader.readLine();
                tokens = line.split("0x");
                //    System.out.println(line);
                for (int i = 1; i < tokens.length; i++) {
                    value = Short.valueOf(tokens[i].substring(0, 2), 16);
                    fwBuffer[index++] = value.byteValue();
                    //   System.out.println(fwBuffer[index-1]);
                }
            }
            // log.info("index" + index);

            lineReader.close();
        } catch (IOException e) {
            close();
            log.warning(e.getMessage());
            throw new IOException("can't load binary Cypress FX2 firmware file " + firmwareFile);
        }
        return fwBuffer;
    }

    @Override
    synchronized public void writeCPLDfirmware(String svfFile) throws HardwareInterfaceException {
        byte[] bytearray;
        int status, index;
        USBIO_DATA_BUFFER dataBuffer = null;

        try {
            bytearray = this.parseHexData(svfFile);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        ProgressMonitor progressMonitor = makeProgressMonitor("Writing CPLD configuration - do not unplug", 0, bytearray.length);


        int result;
        USBIO_CLASS_OR_VENDOR_REQUEST vendorRequest = new USBIO_CLASS_OR_VENDOR_REQUEST();


        int numChunks;

        vendorRequest.Flags = UsbIoInterface.USBIO_SHORT_TRANSFER_OK;
        vendorRequest.Type = UsbIoInterface.RequestTypeVendor;  // this is a vendor, not generic USB, request
        vendorRequest.Recipient = UsbIoInterface.RecipientDevice; // device (not endpoint, interface, etc) receives it
        vendorRequest.RequestTypeReservedBits = 0;    // set these bits to zero for Cypress-specific 'vendor request' rather that user defined
        vendorRequest.Request = VR_DOWNLOAD_FIRMWARE; // this is download/upload firmware request. really it is just a 'fill RAM request'
        vendorRequest.Index = 0;

        //	2) send the firmware to Control Endpoint 0
        // when sending firmware, we need to break up the loaded fimware
        //		into MAX_CONTROL_XFER_SIZE blocks
        //
        // this means:
        //	a) the address to load it to needs to be changed (VendorRequest.Value)
        //	b) need a pointer that moves through FWbuffer (pBuffer)
        //	c) keep track of remaining bytes to transfer (FWsize_left);


        //send all but last chunk
        vendorRequest.Value = 0;			//address of firmware location
        dataBuffer = new USBIO_DATA_BUFFER(MAX_CONTROL_XFER_SIZE);
        dataBuffer.setNumberOfBytesToTransfer(dataBuffer.Buffer().length);

        numChunks = bytearray.length / MAX_CONTROL_XFER_SIZE;  // this is number of full chunks to send
        for (int i = 0; i < numChunks; i++) {
            System.arraycopy(bytearray, i * MAX_CONTROL_XFER_SIZE, dataBuffer.Buffer(), 0, MAX_CONTROL_XFER_SIZE);
            result = gUsbIo.classOrVendorOutRequest(dataBuffer, vendorRequest);
            if (result != USBIO_ERR_SUCCESS) {
                close();
                throw new HardwareInterfaceException("Error on downloading segment number " + i + " of CPLD firmware: " + UsbIo.errorText(result));
            }
            progressMonitor.setProgress(vendorRequest.Value);
            progressMonitor.setNote(String.format("sent %d of %d bytes of CPLD configuration", vendorRequest.Value, bytearray.length));
            vendorRequest.Value += MAX_CONTROL_XFER_SIZE;			//change address of firmware location
            if (progressMonitor.isCanceled()) {
                progressMonitor = makeProgressMonitor("Writing CPLD configuration - do not unplug", 0, bytearray.length);
            }
        }

        // now send final (short) chunk
        int numBytesLeft = bytearray.length % MAX_CONTROL_XFER_SIZE;  // remainder
        if (numBytesLeft > 0) {
            dataBuffer = new USBIO_DATA_BUFFER(numBytesLeft);
            dataBuffer.setNumberOfBytesToTransfer(dataBuffer.Buffer().length);
            //    vendorRequest.Index = 1; // indicate that this is the last chuck, now program CPLD
            System.arraycopy(bytearray, numChunks * MAX_CONTROL_XFER_SIZE, dataBuffer.Buffer(), 0, numBytesLeft);

            // send remaining part of firmware
            result = gUsbIo.classOrVendorOutRequest(dataBuffer, vendorRequest);
            if (result != USBIO_ERR_SUCCESS) {
                close();
                throw new HardwareInterfaceException("Error on downloading final segment of CPLD firmware: " + UsbIo.errorText(result));
            }
        }

        vendorRequest = new USBIO_CLASS_OR_VENDOR_REQUEST();
        dataBuffer = new USBIO_DATA_BUFFER(1);

        vendorRequest.Flags = UsbIoInterface.USBIO_SHORT_TRANSFER_OK;
        vendorRequest.Type = UsbIoInterface.RequestTypeVendor;
        vendorRequest.Recipient = UsbIoInterface.RecipientDevice;
        vendorRequest.RequestTypeReservedBits = 0;
        vendorRequest.Request = VR_DOWNLOAD_FIRMWARE;
        vendorRequest.Index = 1;
        vendorRequest.Value = 0;

        dataBuffer.setNumberOfBytesToTransfer(1);
        status = gUsbIo.classOrVendorOutRequest(dataBuffer, vendorRequest);

        if (status != USBIO_ERR_SUCCESS) {
            log.info(UsbIo.errorText(status));
            try {
                Thread.sleep(2000);
                this.open();
            } catch (Exception e) {
            }
        }

        vendorRequest = new USBIO_CLASS_OR_VENDOR_REQUEST();
        dataBuffer = new USBIO_DATA_BUFFER(10);

        vendorRequest.Flags = UsbIoInterface.USBIO_SHORT_TRANSFER_OK;
        vendorRequest.Type = UsbIoInterface.RequestTypeVendor;
        vendorRequest.Recipient = UsbIoInterface.RecipientDevice;
        vendorRequest.RequestTypeReservedBits = 0;
        vendorRequest.Request = VR_DOWNLOAD_FIRMWARE;
        vendorRequest.Index = 0;
        vendorRequest.Value = 0;

        dataBuffer.setNumberOfBytesToTransfer(10);
        status = gUsbIo.classOrVendorInRequest(dataBuffer, vendorRequest);

        if (status != USBIO_ERR_SUCCESS) {
            throw new HardwareInterfaceException("Unable to receive error code: " + UsbIo.errorText(status));
        }

        HardwareInterfaceException.clearException();

        // log.info("bytes transferred" + dataBuffer.getBytesTransferred());
        if (dataBuffer.getBytesTransferred() == 0) {
            //this.sendVendorRequest(VR_DOWNLOAD_FIRMWARE, (short) 0, (short) 0);
            throw new HardwareInterfaceException("Unable to program CPLD, could not get xsvf Error code");
        }
        progressMonitor.close();

        if (dataBuffer.Buffer()[1] != 0) {
            //this.sendVendorRequest(VR_DOWNLOAD_FIRMWARE, (short) 0, (short) 0);
            int dataindex = (dataBuffer.Buffer()[6] << 24) | (dataBuffer.Buffer()[7] << 16) | (dataBuffer.Buffer()[8] << 8) | (dataBuffer.Buffer()[9]);
            int algoindex = (dataBuffer.Buffer()[2] << 24) | (dataBuffer.Buffer()[3] << 16) | (dataBuffer.Buffer()[4] << 8) | (dataBuffer.Buffer()[5]);
            throw new HardwareInterfaceException("Unable to program CPLD, error code: " + dataBuffer.Buffer()[1] + " algo index: " + algoindex + " data index " + dataindex);
            // System.out.println("Unable to program CPLD, unable to program CPLD, error code: " + dataBuffer.Buffer()[1] + ", at command: " + command + " index: " + index + " commandlength " + commandlength);
        }
    }

    /** 
     * Starts reader buffer pool thread and enables in endpoints for AEs. This method is overridden to construct
    our own reader with its translateEvents method
     */
    @Override
    public void startAEReader() throws HardwareInterfaceException {  // raphael: changed from private to protected, because i need to access this method
        setAeReader(new RetinaAEReader(this));
        allocateAEBuffers();

        getAeReader().startThread(3); // arg is number of errors before giving up
        startADC();
        HardwareInterfaceException.clearException();
    }
    boolean gotY = false; // TODO  hack for debugging state machine

    /**
     * @return the TrackTime
     */
    public int getTrackTime() {
        return TrackTime;
    }

    /**
     * @return the RefOnTime
     */
    public int getRefOnTime() {
        return RefOnTime;
    }

    /**
     * @return the RefOffTime
     */
    public int getRefOffTime() {
        return RefOffTime;
    }

    /**
     * @return the IdleTime
     */
    public int getIdleTime() {
        return IdleTime;
    }

    /**
     * @return the Select5Tbuffer
     */
    public boolean isSelect5Tbuffer() {
        return Select5Tbuffer;
    }

    /**
     * @return the UseCalibration
     */
    public boolean isUseCalibration() {
        return UseCalibration;
    }

    /**
     * @return the ADCchannel
     */
    public int getADCChannel() {
        return ADCchannel;
    }

    /**
     * @return the chipReset
     */
    public boolean isChipReset() {
        return chipReset;
    }


    /** This reader understands the format of raw USB data and translates to the AEPacketRaw */
    public class RetinaAEReader extends CypressFX2.AEReader {

        public RetinaAEReader(CypressFX2 cypress) throws HardwareInterfaceException {
            super(cypress);
        }
        /** Method to translate the UsbIoBuffer for the DVS320 sensor which uses the 32 bit address space.
         *<p>
         * It has a CPLD to timestamp events and uses the CypressFX2 in slave
         * FIFO mode. 
         *<p>The DVS320 has a burst mode readout mechanism that 
         * outputs a row address,
         * then all the latched column addresses.
         *The columns are output left to right. A timestamp is only
         * meaningful at the row addresses level. Therefore
         *the board timestamps on row address, and then 
         * sends the data in the following sequence:
         * timestamp, row, col, col, col,....,timestamp,row,col,col...
         * <p>
         * Intensity information is transmitted by bit 8, which is set by the chip
         *The bit encoding of the data is as follows
         *<literal>
        Address bit	Address bit pattern
        0	LSB Y or Polarity ON=1
        1	Y1 or LSB X
        2	Y2 or X1
        3	Y3 or X2
        4	Y4 or X3
        5	Y5 or X4
        6	Y6 or X5
        7	Y7 (MSBY) or X6
        8	intensity or X7. This bit is set for a Y address if the intensity neuron has spiked. This bit is also X7 for X addreses.
        9	X8 (MSBX)
        10	Y=0, X=1
        </literal>
         *
         * The two msbs of the raw 16 bit data are used to tag the type of data, e.g. address, timestamp, or special events wrap or
         * reset host timestamps.
        <literal>
        Address             Name
        00xx xxxx xxxx xxxx	pixel address
        01xx xxxx xxxx xxxx	timestamp
        10xx xxxx xxxx xxxx	wrap
        11xx xxxx xxxx xxxx	timestamp reset
        </literal>
        
         *The msb of the 16 bit timestamp is used to signal a wrap (the actual timestamp is only 15 bits).
         * The wrapAdd is incremented when an empty event is received which has the timestamp bit 15
         * set to one.
         *<p>
         * Therefore for a valid event only 15 bits of the 16 transmitted timestamp bits are valid, bit 15
         * is the status bit. overflow happens every 32 ms.
         * This way, no roll overs go by undetected, and the problem of invalid wraps doesn't arise.
         *@param minusEventEffect the data buffer
         *@see #translateEvents
         */
        static private final byte Xmask = (byte) 0x01;
        static private final byte IntensityMask = (byte) 0x40;
        private int lasty = 0;
        private int currentts = 0;
        private int lastts = 0;
        private int yonlycount=0;
        private int yonlycons=0;
        private boolean doubleY=false;

        @Override
        protected void translateEvents(UsbIoBuf b) {
            try {
                // data from cDVS is stateful. 2 bytes sent for each word of data can consist of either timestamp, y address, x address, or ADC value.
                // The type of data is determined from bits in these two bytes.

//            if(tobiLogger.isEnabled()==false) tobiLogger.setEnabled(true); //debug
                synchronized (aePacketRawPool) {
                    AEPacketRaw buffer = aePacketRawPool.writeBuffer();

                    int NumberOfWrapEvents;
                    NumberOfWrapEvents = 0;

                    byte[] buf = b.BufferMem;
                    int bytesSent = b.BytesTransferred;
                    if (bytesSent % 2 != 0) {
                        System.err.println("warning: " + bytesSent + " bytes sent, which is not multiple of 2");
                        bytesSent = (bytesSent / 2) * 2; // truncate off any extra part-event
                    }

                    int[] addresses = buffer.getAddresses();
                    int[] timestamps = buffer.getTimestamps();
                    //log.info("received " + bytesSent + " bytes");
                    // write the start of the packet
                    buffer.lastCaptureIndex = eventCounter;
//                     tobiLogger.log("#packet");
                    for (int i = 0; i < bytesSent; i += 2) {
                        //   tobiLogger.log(String.format("%d %x %x",eventCounter,buf[i],buf[i+1])); // DEBUG
                        //   int val=(buf[i+1] << 8) + buf[i]; // 16 bit value of data
                        int dataword = (0xff & buf[i]) | (0xff00 & (buf[i + 1] << 8));  // data sent little endian

                        final int code = (buf[i + 1] & 0xC0) >> 6; // gets two bits at XX00 0000 0000 0000. (val&0xC000)>>>14;
                        //  log.info("code " + code);
                        switch (code) {
                            case 0: // address
                                // If the data is an address, we write out an address value if we either get an ADC reading or an x address.
                                // We also write a (fake) address if
                                // we get two y addresses in a row, which occurs when the on-chip AE state machine doesn't properly function.
                                //  Here we also read y addresses but do not write out any output address until we get either 1) an x-address, or 2)
                                // another y address without intervening x-address.
                                // NOTE that because ADC events do not have a timestamp, the size of the addresses and timestamps data are not the same.
                                // To simplify data structure handling in AEPacketRaw and AEPacketRawPool,
                                // ADC events are timestamped just like address-events. ADC events get the timestamp of the most recently preceeding address-event.
                                // NOTE2: unmasked bits are read as 1's from the hardware. Therefore it is crucial to properly mask bits.
                                if ((eventCounter >= aeBufferSize) || (buffer.overrunOccuredFlag)) {
                                    buffer.overrunOccuredFlag = true; // throw away events if we have overrun the output arrays
                                } else {
                                    if ((dataword & cDVSTest20.ADDRESS_TYPE_MASK) == cDVSTest20.ADDRESS_TYPE_ADC) {
                                        addresses[eventCounter] = dataword;
                                        timestamps[eventCounter] = currentts;  // ADC event gets last timestamp
                                        eventCounter++;
                                        //      System.out.println("ADC word: " + (dataword&cDVSTest20.ADC_DATA_MASK));
                                    } else if ((buf[i + 1] & Xmask) == Xmask) {////  received an X address, write out event to addresses/timestamps output arrays
                                        // x adddress
                                        //xadd = (buf[i] & 0xff);  //
                                        addresses[eventCounter] = (lasty << cDVSTest20.YSHIFT) | (dataword & (cDVSTest20.XMASK | cDVSTest20.POLMASK));  // combine current bits with last y address bits and send
                                        timestamps[eventCounter] = currentts; // add in the wrap offset and convert to 1us tick
                                        eventCounter++;
                                        //    log.info("received x address");
                                        gotY = false;
//                                        if (doubleY)
//                                        {
//                                            doubleY=false;
//                                            System.out.println(yonlycons+ " Y addresses consecutively recieved in cDVSTestHardwareInterface, total y only: "+ yonlycount); // this printout makes display very jerky!!!
//                                            yonlycons=0;
//                                        }
                                    } else {// y address
                                        // lasty = (0xFF & buf[i]); //
//                                        if (gotY) {// TODO creates bogus event to see y without x. This should not normally occur.
//                                  //          addresses[eventCounter] = (lasty << cDVSTest20.YSHIFT) + (cDVSTest20.SIZEX_TOTAL - 1 << 1);                 //(0xffff&((short)buf[i]&0xff | ((short)buf[i+1]&0xff)<<8));
//                                  //          timestamps[eventCounter] = lastts; //*TICK_US; //add in the wrap offset and convert to 1us tick
//                                  //          eventCounter++;
//                                            yonlycount++;
//                                            yonlycons++;
//                                            doubleY=true;
//                                        }
                                        if ((buf[i] & IntensityMask) != 0) { // intensity spike
                                            // log.info("received intensity bit");
                                            addresses[eventCounter] = cDVSTest20.INTENSITYMASK;
                                            timestamps[eventCounter] = currentts;
                                            eventCounter++;
                                        }
                                        lasty = (cDVSTest20.YMASK >>> cDVSTest20.YSHIFT) & dataword; //(0xFF & buf[i]); //
                                        gotY = true;
                                    }
                                }
                                break;
                            case 1: // timestamp
                                lastts = currentts;
                                currentts = ((0x3f & buf[i + 1]) << 8) | (buf[i] & 0xff);
                                currentts = (TICK_US * (currentts + wrapAdd));
                                //           log.info("received timestamp");
                                break;
                            case 2: // wrap
                                wrapAdd += 0x4000L;
                                NumberOfWrapEvents++;
                                //   log.info("wrap");
                                break;
                            case 3: // ts reset event
                                this.resetTimestamps();
                                //   log.info("timestamp reset");
                                break;
                        }
                    } // end for

                    buffer.setNumEvents(eventCounter);
                    // write capture size
                    buffer.lastCaptureLength = eventCounter - buffer.lastCaptureIndex;
                    buffer.systemModificationTimeNs = System.nanoTime();

                    //     log.info("packet size " + buffer.lastCaptureLength + " number of Y addresses " + numberOfY);
                    // if (NumberOfWrapEvents!=0) {
                    //System.out.println("Number of wrap events received: "+ NumberOfWrapEvents);
                    //}
                    //System.out.println("wrapAdd : "+ wrapAdd);
                } // sync on aePacketRawPool
            } catch (java.lang.IndexOutOfBoundsException e) {
                log.warning(e.toString());
            }
        }
    }
}

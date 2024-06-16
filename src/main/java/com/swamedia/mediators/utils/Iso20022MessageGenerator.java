package com.swamedia.mediators.utils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.prowidesoftware.swift.model.mx.AbstractMX;
import com.prowidesoftware.swift.model.mx.MxPain00100103;
import com.prowidesoftware.swift.model.mx.dic.AccountIdentification4Choice;
import com.prowidesoftware.swift.model.mx.dic.ActiveOrHistoricCurrencyAndAmount;
import com.prowidesoftware.swift.model.mx.dic.AmountType3Choice;
import com.prowidesoftware.swift.model.mx.dic.BranchAndFinancialInstitutionIdentification4;
import com.prowidesoftware.swift.model.mx.dic.CashAccount16;
import com.prowidesoftware.swift.model.mx.dic.CreditTransferTransactionInformation10;
import com.prowidesoftware.swift.model.mx.dic.CustomerCreditTransferInitiationV03;
import com.prowidesoftware.swift.model.mx.dic.FinancialInstitutionIdentification7;
import com.prowidesoftware.swift.model.mx.dic.GroupHeader32;
import com.prowidesoftware.swift.model.mx.dic.PaymentIdentification1;
import com.prowidesoftware.swift.model.mx.dic.PaymentInstructionInformation3;
import com.prowidesoftware.swift.model.mx.dic.PaymentMethod3Code;

public class Iso20022MessageGenerator {
	public static String generateIso20022Message(String messageType, Map<String, String> iso20022Data) {
		try {
			// Get the class dynamically based on the message type
			Class<?> mxClass = Class.forName("com.prowidesoftware.swift.model.mx." + messageType);

			// Create an instance of the message class
			AbstractMX mxMessage = (AbstractMX) mxClass.getDeclaredConstructor().newInstance();

			// Use reflection to populate the mxMessage object with iso20022Data
			populateMessage(mxMessage, iso20022Data);

			// Convert the message to XML format
			Method messageMethod = mxClass.getMethod("message");
			return (String) messageMethod.invoke(mxMessage);
		} catch (Exception e) {
			throw new RuntimeException("Error generating ISO 20022 message", e);
		}
	}

	private static void populateMessage(AbstractMX mxMessage, Map<String, String> iso20022Data) throws Exception {
		if (mxMessage instanceof MxPain00100103 && iso20022Data != null) {
			MxPain00100103 pain001 = (MxPain00100103) mxMessage;
			CustomerCreditTransferInitiationV03 ccti = new CustomerCreditTransferInitiationV03();

			GroupHeader32 grpHdr = new GroupHeader32();
			if (iso20022Data.get("GrpHdr.msgId") != null) {
				grpHdr.setMsgId(iso20022Data.get("GrpHdr.msgId"));
			}
			grpHdr.setCreDtTm(OffsetDateTime.now());
			if (iso20022Data.get("GrpHdr.nbOfTxs") != null) {
				grpHdr.setNbOfTxs(iso20022Data.get("GrpHdr.nbOfTxs"));
			}
			ccti.setGrpHdr(grpHdr);

			// PmtInf
			PaymentInstructionInformation3 pmtInf = new PaymentInstructionInformation3();
			// PmtInfId
			if (iso20022Data.get("PmtInf.PmtInfId") != null) {
				pmtInf.setPmtInfId(iso20022Data.get("PmtInf.PmtInfId"));
			}
			// PmtMtd
			if (iso20022Data.get("PmtInf.PmtMtd") != null) {
				pmtInf.setPmtMtd(PaymentMethod3Code.fromValue(iso20022Data.get("PmtInf.PmtMtd")));
			}
			// ReqdExctnDt
			if (iso20022Data.get("PmtInf.ReqdExctnDt") != null) {
				String dateString = iso20022Data.get("PmtInf.ReqdExctnDt");
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyMMdd");
				LocalDate reqdExctnDt = LocalDate.parse(dateString, formatter);
				pmtInf.setReqdExctnDt(reqdExctnDt);
			}

			// DbtrAcct
			if (iso20022Data.get("DbtrAcct.IBAN") != null) {
				CashAccount16 DbtrAcct = new CashAccount16();
				AccountIdentification4Choice Id = new AccountIdentification4Choice();
				Id.setIBAN(iso20022Data.get("DbtrAcct.IBAN"));
				DbtrAcct.setId(Id);
				pmtInf.setDbtrAcct(DbtrAcct);
			}

			// DbtrAgt
			if (iso20022Data.get("DbtrAgt.BIC") != null) {
				BranchAndFinancialInstitutionIdentification4 DbtrAgt = new BranchAndFinancialInstitutionIdentification4();
				FinancialInstitutionIdentification7 FinInstnId = new FinancialInstitutionIdentification7();
				FinInstnId.setBIC(iso20022Data.get("DbtrAgt.BIC"));
				DbtrAgt.setFinInstnId(FinInstnId);
				pmtInf.setDbtrAgt(DbtrAgt);
			}

			// CdtTrfTxInf
			CreditTransferTransactionInformation10 CdtTrfTxInf = new CreditTransferTransactionInformation10();

			// PmtId
			if (iso20022Data.get("CdtTrfTxInf.PmtId.EndToEndId") != null) {
				PaymentIdentification1 PmtId = new PaymentIdentification1();
				PmtId.setEndToEndId(iso20022Data.get("CdtTrfTxInf.PmtId.EndToEndId"));
				CdtTrfTxInf.setPmtId(PmtId);
			}

			// Amt
			if (iso20022Data.get("CdtTrfTxInf.Amt.InstdAmt.Ccy") != null
					&& iso20022Data.get("CdtTrfTxInf.Amt.InstdAmt.Value") != null) {
				AmountType3Choice Amt = new AmountType3Choice();
				// InstdAmt
				ActiveOrHistoricCurrencyAndAmount InstdAmt = new ActiveOrHistoricCurrencyAndAmount();
				InstdAmt.setCcy(iso20022Data.get("CdtTrfTxInf.Amt.InstdAmt.Ccy"));
				InstdAmt.setValue(
						BigDecimal.valueOf(Double.valueOf(iso20022Data.get("CdtTrfTxInf.Amt.InstdAmt.Value"))));
				Amt.setInstdAmt(InstdAmt);
				CdtTrfTxInf.setAmt(Amt);
			}

			pmtInf.getCdtTrfTxInf().add(CdtTrfTxInf);
			ccti.getPmtInf().add(pmtInf);
			pain001.setCstmrCdtTrfInitn(ccti);
		}
	}

}

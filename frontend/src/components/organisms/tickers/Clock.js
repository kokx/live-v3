import { DateTime, Settings } from "luxon";
import React, { useCallback, useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { TextWrap } from "./Text";

Settings.defaultZone = "utc";

export const Clock = ({ tickerSettings }) => {
    const contestInfo = useSelector((state) => state.contestInfo.info);
    const getStatus = useCallback(() => {
        const milliseconds = DateTime.fromMillis(contestInfo.startTimeUnixMs).diffNow().negate().milliseconds *
            (contestInfo.emulationSpeed ?? 0);
        if(milliseconds < 0)
            return "BEFORE";
        if(milliseconds >= contestInfo.contestLengthMs)
            return "OVER";
        return DateTime.fromMillis(milliseconds).toFormat("H:mm:ss");
    }, [contestInfo]);
    const [status, setStatus] = useState(getStatus());
    useEffect(() => {
        const interval = setInterval(() => setStatus(getStatus()), 200);
        return () => clearInterval(interval);
    }, []);
    return <TextWrap>
        {status}
    </TextWrap>;
};

export default Clock;
import { Request, Response, NextFunction } from "express";
import { randomUUID } from "crypto";

export const REQUEST_ID_HEADER = "x-request-id";

export function requestIdMiddleware(
    req: Request,
    res: Response,
    next: NextFunction
) {
    let requestId = req.header(REQUEST_ID_HEADER);
    if (!requestId) {
        requestId = randomUUID();
    }

    (req as any).requestId = requestId;
    res.setHeader(REQUEST_ID_HEADER, requestId);

    next();
}
